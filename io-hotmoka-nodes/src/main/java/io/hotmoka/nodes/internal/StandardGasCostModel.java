package io.hotmoka.nodes.internal;

import java.math.BigInteger;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.nodes.GasCostModel;

/**
 * A specification of the cost of gas.
 */
public class StandardGasCostModel implements GasCostModel {

	@Override
	public int ramCostOfObject() {
		return 8;
	}

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
		return 12;
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
	public BigInteger ramCostForInstallingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 40);
	}

	@Override
	public BigInteger ramCostForLoadingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 200);
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

	@Override
	public BigInteger cpuCostForInstallingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 400);
	}

	@Override
	public BigInteger cpuCostForLoadingJar(int numBytes) {
		return BigInteger.valueOf(numBytes / 1000);
	}

	private final static BigInteger CPU_BASE_TRANSACTION_COST = BigInteger.valueOf(10);

	@Override
	public BigInteger cpuBaseTransactionCost() {
		return CPU_BASE_TRANSACTION_COST;
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
	public BigInteger storageCostOf(TransactionReference transaction) {
		return BigInteger.valueOf(storageCostPerSlot() * 4L);
	}
}