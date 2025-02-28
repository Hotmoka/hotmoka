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

import io.hotmoka.verification.PushersIterators;
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

		if (Const.CONSTRUCTOR_NAME.equals(methodName) && methodArgs.length > 0 && methodIsPayableIn(className))
			for (var ih: instructionsOf(method)) {
				var maybeInvoke = getInvokeToPayableFromContractConstructorOnThis(ih);
				if (maybeInvoke.isPresent() && amountMightBeChanged(ih, maybeInvoke.get()))
					issue(new IllegalModificationOfAmountInConstructorChaining(inferSourceFile(), method.getName(), lineOf(ih)));
			}
	}

	/**
	 * Checks if the given call to the constructor of the superclass, on this, occurs
	 * with a first argument that cannot be proved to be the same amount variable as in the caller, unchanged.
	 * 
	 * @param ih the instruction, that calls the constructor of the superclass
	 * @param invoke the invoke instruction inside {@code ih}
	 * @return true if that condition holds
	 * @throws IllegalJarException if the jar under verification is illegal
	 */
	private boolean amountMightBeChanged(InstructionHandle ih, InvokeInstruction invoke) throws IllegalJarException {
		int slots = Stream.of(invoke.getArgumentTypes(cpg)).mapToInt(Type::getSize).sum();
		return !pusherIsLoad1(ih, slots) || mightUpdateLocal(ih, 1);
	}

	private boolean pusherIsLoad1(InstructionHandle ih, int slots) throws IllegalJarException {
		var it = PushersIterators.of(ih, slots, method);

		while (it.hasNext())
			if (!(it.next().getInstruction() instanceof LoadInstruction load) || load.getIndex() != 1)
				return false;

		return true;
	}

	private boolean pusherIsLoad0(InstructionHandle ih, int slots) throws IllegalJarException {
		var it = PushersIterators.of(ih, slots, method);

		while (it.hasNext())
			if (!(it.next().getInstruction() instanceof LoadInstruction load) || load.getIndex() != 0)
				return false;

		return true;
	}

	private boolean mightUpdateLocal(InstructionHandle ih, int local) throws IllegalJarException {
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
				throw new IllegalJarException("Cannot follow stack pushers");

			for (var targeter: targeters)
				if (targeter instanceof BranchInstruction branch) {
					InstructionHandle added = findInstruction(branch).orElseThrow(() -> new IllegalJarException("Cannot follow the stack pushers"));
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

    private Optional<InvokeInstruction> getInvokeToPayableFromContractConstructorOnThis(InstructionHandle ih) throws IllegalJarException {
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

    			if (callsPayableFromContract && pusherIsLoad0(ih, slots + 1))
    				return Optional.of(invoke);
    		}
    	}

		return Optional.empty();
	}
}