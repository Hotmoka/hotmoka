package io.takamaka.code.instrumentation;

import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.references.TransactionReference;

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

	@Override
	public BigInteger cpuBaseTransactionCost() {
		return BigInteger.valueOf(10);
	}

	@Override
	public int storageCostPerSlot() {
		return 50;
	}

	@Override
	public BigInteger storageCostOf(BigInteger value) {
		return BigInteger.valueOf(storageCostPerSlot()).add(BigInteger.valueOf(value.bitLength() / 2));
	}

	@Override
	public BigInteger storageCostOf(String value) {
		return BigInteger.valueOf(storageCostPerSlot()).add(BigInteger.valueOf(value.length() * 50));
	}

	@Override
	public BigInteger storageCostOfBytes(int numBytes) {
		return BigInteger.valueOf(storageCostPerSlot()).add(BigInteger.valueOf(numBytes * 50));
	}

	@Override
	public BigInteger storageCostOf(TransactionReference transaction) {
		return BigInteger.valueOf(storageCostPerSlot() * 8L);
	}
}