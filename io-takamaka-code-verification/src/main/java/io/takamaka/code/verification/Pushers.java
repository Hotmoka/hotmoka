package io.takamaka.code.verification;

import java.util.stream.Stream;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;

/**
 * A utility objects that allows one to determine the pushers of stack values on the stack.
 */
public interface Pushers {

	/**
	 * Yields the closest instructions that push on the stack the element
	 * whose stack height is equal to the height of the stack at {@code ih} minus {@code slots}.
	 * 
	 * @param ih the start instruction of the look up
	 * @param slots the difference in stack height
	 * @param il the list of instructions where {@code ih} occurs
	 * @param cpg the constant pool generator of the class for which this object works.
	 * @return the instructions
	 */
	public Stream<InstructionHandle> getPushers(InstructionHandle ih, int slots, InstructionList il, ConstantPoolGen cpg);
}