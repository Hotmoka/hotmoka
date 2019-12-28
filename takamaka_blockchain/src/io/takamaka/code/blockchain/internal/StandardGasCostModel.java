package io.takamaka.code.blockchain.internal;

import java.math.BigInteger;

import io.takamaka.code.blockchain.GasCostModel;
import io.takamaka.code.blockchain.TransactionReference;

/**
 * A specification of the cost of gas.
 */
public class StandardGasCostModel implements GasCostModel {

	/**
	 * The standard gas model for instrumentation, that provides standard measurements for most but not all
	 * methods below.
	 */
	private final io.takamaka.code.instrumentation.GasCostModel parent = io.takamaka.code.instrumentation.GasCostModel.standard();

	@Override
	public int ramCostOfObject() {
		return parent.ramCostOfObject();
	}

	@Override
	public int ramCostOfField() {
		return parent.ramCostOfField();
	}

	@Override
	public int ramCostOfArraySlot() {
		return parent.ramCostOfArraySlot();
	}

	@Override
	public int ramCostOfArray() {
		return parent.ramCostOfArray();
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
	public BigInteger ramCostForInstalling(int numBytes) {
		return BigInteger.valueOf(numBytes / 40);
	}

	@Override
	public BigInteger ramCostForLoading(int numBytes) {
		return BigInteger.valueOf(numBytes / 200);
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
	public int cpuCostOfInvokeInstruction() {
		return parent.cpuCostOfInvokeInstruction();
	}

	@Override
	public int cpuCostOfSelectInstruction() {
		return parent.cpuCostOfSelectInstruction();
	}

	@Override
	public int cpuCostOfMemoryAllocationInstruction() {
		return parent.cpuCostOfMemoryAllocationInstruction();
	}

	@Override
	public int cpuCostOfInstruction() {
		return parent.cpuCostOfInstruction();
	}

	@Override
	public BigInteger cpuCostForInstallingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 400);
	}

	@Override
	public BigInteger cpuCostForLoadingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 1000);
	}

	@Override
	public int cpuBaseTransactionCost() {
		return 10;
	}

	@Override
	public BigInteger cpuCostForGettingRequestAt(TransactionReference transaction) {
		return BigInteger.valueOf(10);
	}

	@Override
	public BigInteger cpuCostForGettingResponseAt(TransactionReference transaction) {
		return BigInteger.valueOf(10);
	}

	@Override
	public int storageCostPerSlot() {
		return 10;
	}

	@Override
	public BigInteger storageCostOf(BigInteger value) {
		return BigInteger.valueOf(storageCostPerSlot()).add(BigInteger.valueOf(value.bitLength() / 32));
	}

	@Override
	public BigInteger storageCostOf(String value) {
		return BigInteger.valueOf(storageCostPerSlot()).add(BigInteger.valueOf(value.length() / 4));
	}

	@Override
	public BigInteger storageCostOfJar(int numBytes) {
		return BigInteger.valueOf(storageCostPerSlot()).add(BigInteger.valueOf(numBytes / 4));
	}

	@Override
	public BigInteger toCoin(BigInteger gas) {
		return gas.divide(BigInteger.valueOf(100));
	}
}