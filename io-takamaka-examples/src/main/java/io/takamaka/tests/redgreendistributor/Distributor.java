package io.takamaka.tests.redgreendistributor;

import java.math.BigInteger;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.RedGreenContract;
import io.takamaka.code.lang.RedGreenPayableContract;
import io.takamaka.code.lang.RedPayable;
import io.takamaka.code.util.StorageList;

public class Distributor extends RedGreenContract {
	private final StorageList<RedGreenPayableContract> payees = new StorageList<>();
	private final RedGreenPayableContract owner;

	public @Entry(RedGreenPayableContract.class) Distributor() {
		owner = (RedGreenPayableContract) caller();
	}

	public @Entry(RedGreenPayableContract.class) void addAsPayee() {
		payees.add((RedGreenPayableContract) caller());
	}

	public @Payable @Entry void distributeGreen(BigInteger amount) {
		int size = payees.size();
		if (size > 0) {
			BigInteger eachGets = amount.divide(BigInteger.valueOf(size));
			payees.forEach(payee -> payee.receive(eachGets));
			owner.receive(balance());
		}
	}

	public @RedPayable @Entry void distributeRed(BigInteger amount) {
		int size = payees.size();
		if (size > 0) {
			BigInteger eachGets = amount.divide(BigInteger.valueOf(size));
			payees.forEach(payee -> payee.receiveRed(eachGets));
			owner.receiveRed(balanceRed());
		}
	}
}