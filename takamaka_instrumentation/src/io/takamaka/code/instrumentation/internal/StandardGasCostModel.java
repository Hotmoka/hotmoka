package io.takamaka.code.instrumentation.internal;

import io.takamaka.code.instrumentation.GasCostModel;

/**
 * A specification of the cost of gas.
 */
public class StandardGasCostModel implements GasCostModel {

	@Override
	public int ramCostOfField() {
		return 4;
	}

	@Override
	public int ramCostOfArraySlot() {
		return 4;
	}

	@Override
	public int ramCostOfArray() {
		return 8;
	}

	@Override
	public int ramCostOfActivationRecord() {
		return 4;
	}

	@Override
	public int ramCostOfActivationSlot() {
		return 4;
	}

	@Override
	public int cpuCostOfArithmeticInstruction() {
		return 2;
	}

	@Override
	public int cpuCostOfArrayAccessInstruction() {
		return 3;
	}

	@Override
	public int cpuCostOfFieldAccessInstruction() {
		return 3;
	}

	@Override
	public int cpuCostOfInvokeInstruction() {
		return 5;
	}

	@Override
	public int cpuCostOfSelectInstruction() {
		return 4;
	}

	@Override
	public int cpuCostOfMemoryAllocationInstruction() {
		return 10;
	}

	@Override
	public int cpuCostOfInstruction() {
		return 1;
	}
}