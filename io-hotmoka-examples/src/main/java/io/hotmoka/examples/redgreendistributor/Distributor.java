package io.hotmoka.examples.redgreendistributor;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.RedGreenContract;
import io.takamaka.code.lang.RedGreenPayableContract;
import io.takamaka.code.lang.RedPayable;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageLinkedList;

public class Distributor extends RedGreenContract {
	private final StorageList<RedGreenPayableContract> payees = new StorageLinkedList<>();
	private final RedGreenPayableContract owner;

	public @FromContract(RedGreenPayableContract.class) Distributor() {
		owner = (RedGreenPayableContract) caller();
	}

	public @FromContract(RedGreenPayableContract.class) void addAsPayee() {
		payees.add((RedGreenPayableContract) caller());
	}

	public @Payable @FromContract void distributeGreen(BigInteger amount) {
		int size = payees.size();
		if (size > 0) {
			BigInteger eachGets = amount.divide(BigInteger.valueOf(size));
			payees.forEach(payee -> payee.receive(eachGets));
			owner.receive(balance());
		}
	}

	public @RedPayable @FromContract void distributeRed(BigInteger amount) {
		int size = payees.size();
		if (size > 0) {
			BigInteger eachGets = amount.divide(BigInteger.valueOf(size));
			payees.forEach(payee -> payee.receiveRed(eachGets));
			owner.receiveRed(balanceRed());
		}
	}
}