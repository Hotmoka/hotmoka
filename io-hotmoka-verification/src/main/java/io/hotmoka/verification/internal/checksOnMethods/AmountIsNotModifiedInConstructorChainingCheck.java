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
import org.apache.bcel.generic.Type;

import io.hotmoka.verification.Pushers;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.errors.IllegalModificationOfAmountInConstructorChaining;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A checks that {@code @@Payable} constructor-chaining calls pass exactly the same amount passed to the caller.
 */
public class AmountIsNotModifiedInConstructorChainingCheck extends CheckOnMethods {

	public AmountIsNotModifiedInConstructorChainingCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws IllegalJarException {
		super(builder, method);

		if (Const.CONSTRUCTOR_NAME.equals(methodName) && methodArgs.length > 0 && methodIsPayableIn(className)) {
			check(IllegalJarException.class, () ->
				instructions()
				.filter(uncheck(IllegalJarException.class, this::callsPayableFromContractConstructorOnThis))
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
		var invoke = (InvokeInstruction) instruction;
		Type[] argumentTypes = invoke.getArgumentTypes(cpg);
		int slots = Stream.of(argumentTypes).mapToInt(Type::getSize).sum();

		boolean doesNotUseSameLocal = Pushers.of(ih, slots, method)
			.map(InstructionHandle::getInstruction)
			.anyMatch(ins -> !(ins instanceof LoadInstruction load) || load.getIndex() != 1);	

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
				if (previousIns instanceof IINC iinc && iinc.getIndex() == local)
					return true;

				if (seen.add(previous))
					workingSet.add(previous);
			}

			// we proceed with the instructions that jump at currentIh
			InstructionTargeter[] targeters = currentIh.getTargeters();
			if (Stream.of(targeters).anyMatch(targeter -> targeter instanceof CodeExceptionGen))
				throw new IllegalStateException("Cannot follow stack pushers");

			for (var targeter: targeters)
				if (targeter instanceof BranchInstruction branch) {
					InstructionHandle added = findInstruction(branch).orElseThrow(() -> new IllegalStateException("Cannot follow stack pushers"));
					if (seen.add(added))
						workingSet.add(added);
				}
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

    private boolean callsPayableFromContractConstructorOnThis(InstructionHandle ih) throws IllegalJarException {
    	if (ih.getInstruction() instanceof INVOKESPECIAL invoke) {
    		String methodName = invoke.getMethodName(cpg);
    		if (Const.CONSTRUCTOR_NAME.equals(methodName) && invoke.getReferenceType(cpg) instanceof ObjectType receiver) {
    			Type[] argumentTypes = invoke.getArgumentTypes(cpg);
    			int slots = Stream.of(argumentTypes).mapToInt(Type::getSize).sum();
    			String classNameOfReceiver = receiver.getClassName();
    			Type returnType = invoke.getReturnType(cpg);
    			boolean callsPayableFromContract;

    			try {
    				callsPayableFromContract = annotations.isFromContract(classNameOfReceiver, methodName, argumentTypes, returnType) &&
    						annotations.isPayable(classNameOfReceiver, methodName, argumentTypes, returnType);
    			}
    			catch (ClassNotFoundException e) {
    				throw new IllegalJarException(e);
    			}

    			return callsPayableFromContract &&
    					Pushers.of(ih, slots + 1, method)
    					.map(InstructionHandle::getInstruction)
    					.allMatch(ins -> ins instanceof LoadInstruction load && load.getIndex() == 0);	
    		}
    	}

		return false;
	}
}