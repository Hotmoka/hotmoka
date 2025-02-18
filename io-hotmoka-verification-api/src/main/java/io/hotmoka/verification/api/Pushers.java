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

package io.hotmoka.verification.api;

import java.util.stream.Stream;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;

/**
 * A utility objects that allows one to determine the pushers of stack values.
 */
public interface Pushers {

	/**
	 * Yields the closest instructions that push on the stack the element
	 * whose stack height is equal to the height of the stack at {@code ih} minus {@code slots}.
	 * 
	 * @param ih the start instruction of the look-up
	 * @param slots the difference in stack height
	 * @param il the list of instructions where {@code ih} occurs
	 * @param cpg the constant pool generator of the class for which this object works.
	 * @return the instructions
	 */
	Stream<InstructionHandle> getPushers(InstructionHandle ih, int slots, InstructionList il, ConstantPoolGen cpg);
}