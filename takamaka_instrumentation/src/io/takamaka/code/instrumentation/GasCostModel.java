package io.takamaka.code.instrumentation;

import io.takamaka.code.instrumentation.internal.StandardGasCostModel;

/**
 * A specification of the cost of gas.
 */
public interface GasCostModel {

	/**
	 * Yields a cost model that provides standard measurements for gas consumption.
	 * 
	 * @return the standard cost model
	 */
	static GasCostModel standard() {
		return new StandardGasCostModel();
	}

	/**
	 * Yields the RAM cost of a single field of an object allocated in memory. This does not consider the
	 * objects reachable from the field, if any.
	 * 
	 * @return the cost
	 */
	int ramCostOfField();

	/**
	 * Yields the RAM cost of a single element of an array.
	 * 
	 * @return the cost
	 */
	int ramCostOfArraySlot();

	/**
	 * Yields the RAM cost of an array, without considering its elements.
	 * 
	 * @return the cost
	 */
	int ramCostOfArray();

	/**
	 * Yields the RAM cost of a single variable inside an activation record.
	 * 
	 * @return the cost
	 */
	int ramCostOfActivationRecord();

	/**
	 * Yields the RAM cost of a single variable inside an activation record.
	 * 
	 * @return the cost
	 */
	int ramCostOfActivationSlot();

	/**
	 * Yields the CPU gas cost of the execution of an arithmetic instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfArithmeticInstruction();

	/**
	 * Yields the CPU gas cost of the execution of an array access instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfArrayAccessInstruction();

	/**
	 * Yields the CPU gas cost of the execution of a field access instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfFieldAccessInstruction();

	/**
	 * Yields the CPU gas cost of the execution of an invoke instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfInvokeInstruction();

	/**
	 * Yields the CPU gas cost of the execution of a select instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfSelectInstruction();

	/**
	 * Yields the CPU gas cost of the execution of a memory allocation instruction.
	 * 
	 * @return the cost
	 */
	int cpuCostOfMemoryAllocationInstruction();

	/**
	 * Yields the CPU gas cost of the execution of an instruction that does not fall
	 * in any more specific category.
	 * 
	 * @return the cost
	 */
	int cpuCostOfInstruction();
}