/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.verification.internal.checksOnMethods;

import static io.hotmoka.exceptions.CheckRunnable.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import io.hotmoka.exceptions.UncheckedClassNotFoundException;
import io.hotmoka.verification.errors.IllegalModificationOfAmountInConstructorChaining;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A checks that {@code @@Payable} or {@code @@RedPayable} constructor-chaining calls pass exactly
 * the same amount passed to the caller.
 */
public class AmountIsNotModifiedInConstructorChaining extends CheckOnMethods {

	public AmountIsNotModifiedInConstructorChaining(VerifiedClassImpl.Verification builder, MethodGen method) throws ClassNotFoundException {
		super(builder, method);

		if (Const.CONSTRUCTOR_NAME.equals(methodName) && methodArgs.length > 0 && annotations.isPayable(className, methodName, methodArgs, methodReturnType)) {
			check(UncheckedClassNotFoundException.class, () ->
				instructions()
					.filter(uncheck(this::callsPayableFromContractConstructorOnThis))
					.filter(this::amountMightBeChanged)
					.map(ih -> new IllegalModificationOfAmountInConstructorChaining(inferSourceFile(), method.getName(), lineOf(method, ih)))
					.forEachOrdered(this::issue)
			);
		}
	}

	/**
	 * Checks if the given call to the constructor of the superclass, on this, occurs
	 * with a first argument that cannot be proved to be the same amount variable as in the caller, unchanged.
	 * 
	 * @param ih the instruction, that calls the constructor of the superclass
	 * @return true if that condition holds
	 */
	private boolean amountMightBeChanged(InstructionHandle ih) {
		Instruction instruction = ih.getInstruction();
		InvokeInstruction invoke = (InvokeInstruction) instruction;
		Type[] argumentTypes = invoke.getArgumentTypes(cpg);
		int slots = Stream.of(argumentTypes).mapToInt(Type::getSize).sum();

		boolean doesNotUseSameLocal = pushers.getPushers(ih, slots, method.getInstructionList(), cpg)
			.map(InstructionHandle::getInstruction)
			.anyMatch(ins -> !(ins instanceof LoadInstruction) || ((LoadInstruction) ins).getIndex() != 1);	

		return doesNotUseSameLocal || mightUpdateLocal(ih, 1);
	}

	private boolean mightUpdateLocal(InstructionHandle ih, int local) {
		Set<InstructionHandle> seen = new HashSet<>();
		List<InstructionHandle> workingSet = new ArrayList<>();
		workingSet.add(ih);
		seen.add(ih);

		do {
			InstructionHandle currentIh = workingSet.remove(workingSet.size() - 1);
			InstructionHandle previous = currentIh.getPrev();

			if (previous != null) {
				Instruction previousIns = previous.getInstruction();
				if (previousIns instanceof IINC && ((IINC) previousIns).getIndex() == local)
					return true;

				if (seen.add(previous))
					workingSet.add(previous);
			}

			// we proceed with the instructions that jump at currentIh
			InstructionTargeter[] targeters = currentIh.getTargeters();
			if (Stream.of(targeters).anyMatch(targeter -> targeter instanceof CodeExceptionGen))
				throw new IllegalStateException("Cannot follow stack pushers");

			Stream.of(targeters).filter(targeter -> targeter instanceof BranchInstruction)
				.map(targeter -> (BranchInstruction) targeter)
				.forEachOrdered(branch -> {
					Optional<InstructionHandle> added = findInstruction(branch);
					if (added.isEmpty())
						throw new IllegalStateException("Cannot follow stack pushers");
					else
						if (seen.add(added.get()))
							workingSet.add(added.get());
			});
		}
		while (!workingSet.isEmpty());

		return false;
	}

	/**
     * Search for given instruction reference, start at beginning of list.
     *
     * @param i instruction to search for
     * @return instruction the instruction handle
     */
    private Optional<InstructionHandle> findInstruction(final Instruction i) {
    	return instructions().filter(ih -> ih.getInstruction() == i).findFirst();
    }

    private boolean callsPayableFromContractConstructorOnThis(InstructionHandle ih) throws ClassNotFoundException {
		Instruction instruction = ih.getInstruction();
		if (instruction instanceof INVOKESPECIAL) {
			InvokeInstruction invoke = (InvokeInstruction) instruction;
			String methodName = invoke.getMethodName(cpg);
			if (Const.CONSTRUCTOR_NAME.equals(methodName)) {
				Type[] argumentTypes = invoke.getArgumentTypes(cpg);
				ReferenceType receiver = invoke.getReferenceType(cpg);
				if (receiver instanceof ObjectType) {
					int slots = Stream.of(argumentTypes).mapToInt(Type::getSize).sum();
					String classNameOfReceiver = ((ObjectType) receiver).getClassName();
					Type returnType = invoke.getReturnType(cpg);
					boolean callsPayableFromContract = annotations.isFromContract(classNameOfReceiver, methodName, argumentTypes, returnType) &&
						annotations.isPayable(classNameOfReceiver, methodName, argumentTypes, returnType);

					return callsPayableFromContract &&
						pushers.getPushers(ih, slots + 1, method.getInstructionList(), cpg)
							.map(InstructionHandle::getInstruction)
							.allMatch(ins -> ins instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0);	
				}
			}
		}

		return false;
	}
}