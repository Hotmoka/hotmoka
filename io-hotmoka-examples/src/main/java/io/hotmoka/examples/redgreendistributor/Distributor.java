package io.hotmoka.examples.redgreendistributor;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.RedPayable;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;

public class Distributor extends Contract {
	private final StorageList<PayableContract> payees = new StorageLinkedList<>();
	private final PayableContract owner;

	public @FromContract(PayableContract.class) Distributor() {
		owner = (PayableContract) caller();
	}

	public @FromContract(PayableContract.class) void addAsPayee() {
		payees.add((PayableContract) caller());
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