package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.nodes.GasCostModel;

/**
 * An adapter of a HotMoka gas cost model into an instrumentation gas cost model.
 */
class GasCostModelAdapter implements io.takamaka.code.instrumentation.GasCostModel {
	private final GasCostModel parent;

	/**
	 * Adapts the given HotMoka gas cost model.
	 * 
	 * @param parent the HotMoka gas cost model
	 */
	GasCostModelAdapter(GasCostModel parent) {
		this.parent = parent;
	}

	@Override
	public int cpuCostOfArithmeticInstruction() {
		return parent.cpuCostOfArithmeticInstruction();
	}

	@Override
	public int cpuCostOfArrayAccessInstruction() {
		return parent.cpuCostOfArrayAccessInstruction();
	}

	@Override
	public int cpuCostOfFieldAccessInstruction() {
		return parent.cpuCostOfFieldAccessInstruction();
	}

	@Override
	public int cpuCostOfInstruction() {
		return parent.cpuCostOfInstruction();
	}

	@Override
	public int cpuCostOfInvokeInstruction() {
		return parent.cpuCostOfInvokeInstruction();
	}

	@Override
	public int cpuCostOfMemoryAllocationInstruction() {
		return parent.cpuCostOfMemoryAllocationInstruction();
	}

	@Override
	public int cpuCostOfSelectInstruction() {
		return parent.cpuCostOfSelectInstruction();
	}

	@Override
	public int ramCostOfActivationRecord() {
		return parent.ramCostOfActivationRecord();
	}

	@Override
	public int ramCostOfActivationSlot() {
		return parent.ramCostOfActivationSlot();
	}

	@Override
	public int ramCostOfArray() {
		return parent.ramCostOfArray();
	}

	@Override
	public int ramCostOfArraySlot() {
		return parent.ramCostOfArraySlot();
	}

	@Override
	public int ramCostOfField() {
		return parent.ramCostOfField();
	}

	@Override
	public int ramCostOfObject() {
		return parent.ramCostOfObject();
	}
}