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
 * The implementation of the computation of the pushers of a stack value.
 */
public class PushersImpl {

	/**
	 * The start instruction of the look-up.
	 */
	private final InstructionHandle ih;

	/**
	 * The difference in stack height.
	 */
	private final int slots;

	/**
	 * The list of instructions where {@link #ih} occurs.
	 */
	private final InstructionList il;

	/**
	 * The constant pool generator of the class for which this object works.
	 */
	private final ConstantPoolGen cpg;

	/**
	 * Creates a utility class that yields the pushers of values on the stack.
	 * 
	 * @param ih the start instruction of the look-up
	 * @param slots the difference in stack height
	 * @param method the method where {@code ih} occurs
	 */
	public PushersImpl(InstructionHandle ih, int slots, MethodGen method) {
		this.ih = ih;
		this.slots = slots;
		this.il = method.getInstructionList();
		this.cpg = method.getConstantPool();
	}

	public PushersIterator iterator() throws IllegalJarException {
		return new MyIterator(ih, slots);
	}

	/**
	 * This iterator provides the results on demand. This is very important in order to
	 * implement a lazy {@link PushersImpl#getPushers(InstructionHandle, int, InstructionList, ConstantPoolGen)}.
	 */
	private class MyIterator implements PushersIterator {

		private static class HeightAtBytecode {
			public final InstructionHandle ih;
			public final int stackHeightBeforeBytecode;

			public HeightAtBytecode(InstructionHandle ih, int stackHeightBeforeBytecode) {
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

		private final List<HeightAtBytecode> workingSet = new ArrayList<>();
		private final Set<HeightAtBytecode> seen = new HashSet<>();
		private final List<InstructionHandle> results = new ArrayList<>();
		private final Set<InstructionHandle> seenResults = new HashSet<>();

		private MyIterator(InstructionHandle ih, int slots) throws IllegalJarException {
			var start = new HeightAtBytecode(ih, slots);
			workingSet.add(start);
			seen.add(start);
			propagate();
		}

		private void propagate() throws IllegalJarException { // TODO: define maximal effort in propagation
			while (results.isEmpty() && !workingSet.isEmpty()) {
				HeightAtBytecode current = workingSet.remove(workingSet.size() - 1);
				InstructionHandle currentIh = current.ih;
				InstructionHandle previous = currentIh.getPrev();

				List<InstructionHandle> predecessors = new ArrayList<>();
				if (previous != null) {
					Instruction previousIns = previous.getInstruction();
					if (!(previousIns instanceof ReturnInstruction) && !(previousIns instanceof ATHROW) && !(previousIns instanceof GotoInstruction))
						predecessors.add(previous);
				}

				// we proceed with the instructions that jump at currentIh
				for (var targeter: currentIh.getTargeters())
					if (targeter instanceof CodeExceptionGen)
						throw new IllegalJarException("Cannot find stack pushers");
					else if (targeter instanceof BranchInstruction bi)
						predecessors.add(findInstruction(bi).orElseThrow(() -> new IllegalJarException("Cannot find stack pushers")));

				predecessors.forEach(p -> process(p, current.stackHeightBeforeBytecode));
			}
		}

		private void process(InstructionHandle ih, int stackHeightBefore) {
			Instruction ins = ih.getInstruction();
			stackHeightBefore -= ins.produceStack(cpg);

			if (stackHeightBefore <= 0) {
				if (seenResults.add(ih))
					results.add(ih);
			}
			else {
				stackHeightBefore += ins.consumeStack(cpg);
				var added = new HeightAtBytecode(ih, stackHeightBefore);

				if (seen.add(added))
					workingSet.add(added);
			}
		}

		/**
		 * Search for given instruction reference, start at beginning of list.
		 *
		 * @param i instruction to search for
		 * @return instruction the instruction handle
		 */
		private Optional<InstructionHandle> findInstruction(Instruction i) {
			Stream<InstructionHandle> instructions = il == null ? Stream.empty() : StreamSupport.stream(il.spliterator(), false);
			return instructions.filter(ih -> ih.getInstruction() == i).findFirst();
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
}