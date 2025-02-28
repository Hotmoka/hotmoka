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

package io.hotmoka.verification.internal;

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
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.PushersIterator;

/**
 * The implementation of an iterator over the pushers of a stack value.
 */
public class PushersIteratorImpl implements PushersIterator {

	/**
	 * The list of instructions where the pushers are looked for.
	 */
	private final InstructionList il;

	/**
	 * The constant pool generator of the class where {@link #il} occurs.
	 */
	private final ConstantPoolGen cpg;

	/**
	 * The stack elements that still can be propagated backwards in the lookup algorithm.
	 * It's important to use an ordered collection in order to get a deterministic behavior.
	 */
	private final List<HeightAtBytecode> workingSet = new ArrayList<>();

	/**
	 * All {@link #workingSet} generated so far, also those that have been eliminated
	 * from {@link #workingSet} because of iteration, for quick containment test and to
	 * avoid infinite looping.
	 */
	private final Set<HeightAtBytecode> seenWorkingSet = new HashSet<>();

	/**
	 * The stack pushers that have been found but not yet iterated upon.
	 * When this becomes empty, during the iteration, new stack pushers are looked for
	 * starting from {@link #workingSet}, if it is not empty.
	 * It's important to use an ordered collection in order to get a deterministic behavior.
	 */
	private final List<InstructionHandle> results = new ArrayList<>();

	/**
	 * All {@link #results} generated so far, also those that have been eliminated
	 * from {@link #results} because of iteration, for quick containment test and to
	 * avoid infinite looping.
	 */
	private final Set<InstructionHandle> seenResults = new HashSet<>();

	/**
	 * The maximal number of bytecodes that can be considered during the lookup.
	 * This is important to bound the execution time of this lookup algorithm.
	 * For normal programs, it's enough to consider only a few bytecodes to find the pushers
	 * of a stack element, hence this limit is not so relevant.
	 */
	private final static int MAX_BYTECODES = 1000;

	/**
	 * Creates an iterator over the pushers of a stack value.
	 * 
	 * @param ih the start instruction of the look-up
	 * @param slots the difference in stack height from the top of the stack at {@code ih}:
	 *              0 means the top of the stack, 1 means the element below it and so on
	 * @param method the method where {@code ih} occurs
	 * @throws IllegalJarException if the jar of the code where the pushers are computed is illegal or too complex
	 */
	public PushersIteratorImpl(InstructionHandle ih, int slots, MethodGen method) throws IllegalJarException {
		this.il = method.getInstructionList();
		this.cpg = method.getConstantPool();
		var start = new HeightAtBytecode(ih, slots);
		workingSet.add(start);
		seenWorkingSet.add(start);
		propagate();
	}

	/**
	 * The identifier of a stack value: instruction where it is considered and
	 * offset from the top of the stack before the execution of the instruction.
	 */
	private static class HeightAtBytecode {
		private final InstructionHandle ih;
		private final int stackHeightBeforeBytecode;

		private HeightAtBytecode(InstructionHandle ih, int stackHeightBeforeBytecode) {
			this.ih = ih;
			this.stackHeightBeforeBytecode = stackHeightBeforeBytecode;
		}

		@Override
		public String toString() {
			return ih + " with " + stackHeightBeforeBytecode + " stack elements";
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof HeightAtBytecode hab && hab.ih == ih
				&& hab.stackHeightBeforeBytecode == stackHeightBeforeBytecode;
		}

		@Override
		public int hashCode() {
			return ih.getPosition() ^ stackHeightBeforeBytecode;
		}
	}

	/**
	 * This implements a breath-first backward look-up of the stack pushers.
	 */
	private void propagate() throws IllegalJarException {
		while (results.isEmpty() && !workingSet.isEmpty()) {
			HeightAtBytecode current = workingSet.remove(workingSet.size() - 1);
			InstructionHandle currentIh = current.ih;
			InstructionHandle previous = currentIh.getPrev();

			var predecessors = new ArrayList<InstructionHandle>();
			if (previous != null) {
				Instruction previousIns = previous.getInstruction();
				if (!(previousIns instanceof ReturnInstruction) && !(previousIns instanceof ATHROW) && !(previousIns instanceof GotoInstruction))
					predecessors.add(previous);
			}

			// we proceed with the instructions that jump at currentIh
			for (var targeter: currentIh.getTargeters())
				if (targeter instanceof CodeExceptionGen)
					throw new IllegalJarException("Cannot find stack pushers because I do not follow the exception handlers");
				else if (targeter instanceof BranchInstruction bi)
					predecessors.add(findInstruction(bi).orElseThrow(() -> new IllegalJarException("Cannot find stack pushers")));

			for (var predecessor: predecessors)
				process(predecessor, current.stackHeightBeforeBytecode);
		}
	}

	private void process(InstructionHandle ih, int stackHeightBefore) throws IllegalJarException {
		Instruction ins = ih.getInstruction();
		stackHeightBefore -= ins.produceStack(cpg);

		// the instruction might produce more stack elements than the offset from the top of the stack
		// that we are interested in, hence <= is correct
		if (stackHeightBefore <= 0) {
			if (seenResults.add(ih))
				results.add(ih);
		}
		else {
			stackHeightBefore += ins.consumeStack(cpg);
			var added = new HeightAtBytecode(ih, stackHeightBefore);

			if (seenWorkingSet.add(added)) {
				workingSet.add(added);

				if (seenWorkingSet.size() > MAX_BYTECODES)
					throw new IllegalJarException("The stack pushers lookup is too complex: I give up");
			}
		}
	}

	/**
	 * Search for given instruction reference, start at beginning of list.
	 *
	 * @param ins instruction to search for
	 * @return instruction the instruction handle, if any
	 */
	private Optional<InstructionHandle> findInstruction(Instruction ins) {
		Stream<InstructionHandle> instructions = il == null ? Stream.empty() : StreamSupport.stream(il.spliterator(), false);
		return instructions.filter(ih -> ih.getInstruction() == ins).findFirst();
	}

	@Override
	public boolean hasNext() {
		return !results.isEmpty();
	}

	@Override
	public InstructionHandle next() throws IllegalJarException {
		InstructionHandle next = results.remove(results.size() - 1);
		propagate();
		return next;
	}
}