/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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