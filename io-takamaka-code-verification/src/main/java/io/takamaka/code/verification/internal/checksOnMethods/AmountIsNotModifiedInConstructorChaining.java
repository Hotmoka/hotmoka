package io.takamaka.code.verification.internal.checksOnMethods;

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

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalModificationOfAmountInConstructorChaining;

/**
 * A checks that {@code @@Payable} or {@code @@RedPayable} constructor-chaining calls pass exactly
 * the same amount passed to the caller.
 */
public class AmountIsNotModifiedInConstructorChaining extends CheckOnMethods {

	public AmountIsNotModifiedInConstructorChaining(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

		if (Const.CONSTRUCTOR_NAME.equals(methodName) && methodArgs.length > 0 && annotations.isPayable(className, methodName, methodArgs, methodReturnType)) {
			instructions()
				.filter(this::callsPayableFromContractConstructorOnThis)
				.filter(this::amountMightBeChanged)
				.map(ih -> new IllegalModificationOfAmountInConstructorChaining(inferSourceFile(), method.getName(), lineOf(method, ih)))
				.forEachOrdered(this::issue);
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

		Runnable error = () -> {
			throw new IllegalStateException("Cannot find stack pushers");
		};

		boolean doesNotUseSameLocal = pushers.getPushers(ih, slots, method.getInstructionList(), cpg, error)
			.map(InstructionHandle::getInstruction)
			.anyMatch(ins -> !(ins instanceof LoadInstruction) || ((LoadInstruction) ins).getIndex() != 1);	

		if (doesNotUseSameLocal)
			return true;

		if (mightUpdateLocal(ih, 1, error))
			return true;

		return false;
	}

	private boolean mightUpdateLocal(InstructionHandle ih, int local, Runnable ifCannotFollow) {
		Set<InstructionHandle> seen = new HashSet<>();
		List<InstructionHandle> workingSet = new ArrayList<>();
		InstructionHandle start = ih;
		workingSet.add(start);
		seen.add(start);

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
				ifCannotFollow.run();

			Stream.of(targeters).filter(targeter -> targeter instanceof BranchInstruction)
				.map(targeter -> (BranchInstruction) targeter)
				.forEachOrdered(branch -> {
					Optional<InstructionHandle> added = findInstruction(branch);
					if (added.isEmpty())
						ifCannotFollow.run();
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

    private boolean callsPayableFromContractConstructorOnThis(InstructionHandle ih) {
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

					if (callsPayableFromContract) {
						Runnable error = () -> {
							throw new IllegalStateException("Cannot find stack pushers");
						};

						return pushers.getPushers(ih, slots + 1, method.getInstructionList(), cpg, error)
							.map(InstructionHandle::getInstruction)
							.allMatch(ins -> ins instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0);	
					}
				}
			}
		}

		return false;
	}
}