package io.takamaka.code.verification.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.ReturnInstruction;

import io.takamaka.code.verification.Pushers;

public class PushersImpl implements Pushers {

	/**
	 * Creates a utility class that yields the pushers of values on the stack,
	 * for the code in a given class.
	 * 
	 * @param clazz the class for whose code this utility works
	 */
	PushersImpl(VerifiedClassImpl clazz) {
	}

	@Override
	public Stream<InstructionHandle> getPushers(InstructionHandle ih, int slots, InstructionList il, ConstantPoolGen cpg, Runnable ifCannotFollow) {
		// TODO: make it lazy
		Set<InstructionHandle> result = new HashSet<>();
		Set<HeightAtBytecode> seen = new HashSet<>();
		List<HeightAtBytecode> workingSet = new ArrayList<>();
		HeightAtBytecode start = new HeightAtBytecode(ih, slots);
		workingSet.add(start);
		seen.add(start);

		do {
			HeightAtBytecode current = workingSet.remove(workingSet.size() - 1);
			InstructionHandle currentIh = current.ih;
			InstructionHandle previous = currentIh.getPrev();

			if (previous != null) {
				Instruction previousIns = previous.getInstruction();
				if (!(previousIns instanceof ReturnInstruction) && !(previousIns instanceof ATHROW) && !(previousIns instanceof GotoInstruction)) {
					// we proceed with previous
					int stackHeightBefore = current.stackHeightBeforeBytecode;
					stackHeightBefore -= previousIns.produceStack(cpg);
					if (stackHeightBefore <= 0)
						result.add(previous);
					else {
						stackHeightBefore += previousIns.consumeStack(cpg);

						HeightAtBytecode added = new HeightAtBytecode(previous, stackHeightBefore);
						if (seen.add(added))
							workingSet.add(added);
					}
				}
			}

			// we proceed with the instructions that jump at currentIh
			InstructionTargeter[] targeters = currentIh.getTargeters();
			if (Stream.of(targeters).anyMatch(targeter -> targeter instanceof CodeExceptionGen))
				ifCannotFollow.run();

			Stream.of(targeters).filter(targeter -> targeter instanceof BranchInstruction)
			.map(targeter -> (BranchInstruction) targeter).forEach(branch -> {
				int stackHeightBefore = current.stackHeightBeforeBytecode;
				stackHeightBefore -= branch.produceStack(cpg);
				if (stackHeightBefore <= 0)
					result.add(previous);
				else {
					stackHeightBefore += branch.consumeStack(cpg);

					Optional<InstructionHandle> branchIH = findInstruction(il, branch);
					if (branchIH.isEmpty())
						ifCannotFollow.run();
					else {
						HeightAtBytecode added = new HeightAtBytecode(branchIH.get(), stackHeightBefore);
						if (seen.add(added))
							workingSet.add(added);
					}
				}
			});
		}
		while (!workingSet.isEmpty());

		return result.stream();
	}

	/**
     * Search for given instruction reference, start at beginning of list.
     *
     * @param i instruction to search for
     * @return instruction the instruction handle
     */
    private Optional<InstructionHandle> findInstruction(InstructionList il, Instruction i) {
    	Stream<InstructionHandle> instructions = il == null ? Stream.empty() : StreamSupport.stream(il.spliterator(), false);
    	return instructions.filter(ih -> ih.getInstruction() == i).findFirst();
    }
}