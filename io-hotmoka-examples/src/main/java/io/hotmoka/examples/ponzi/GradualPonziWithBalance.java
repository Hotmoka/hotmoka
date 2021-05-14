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

package io.hotmoka.examples.ponzi;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.StorageLinkedList;

/**
 * A contract for a Ponzi investment scheme:
 * It involves taking the money sent by the current investor
 * and transferring it to the previous investors. Each investor
 * gets payed back gradually as soon as new investors arrive.
 * 
 * This example is translated from a Solidity contract shown
 * in "Building Games with Ethereum Smart Contracts", by Iyer and Dannen,
 * page 150, Apress 2018.
 */
public class GradualPonziWithBalance extends Contract {
	private final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(1_000L);

	/**
	 * All investors up to now. This list might contain the same investor
	 * many times, which is important to pay it back more than investors
	 * who only invested ones. Hence this list is not the list of keys
	 * of the {@code balances} map, which does not account for repetitions.
	 */
	private final StorageList<PayableContract> investors = new StorageLinkedList<>();

	/**
	 * A map from each investor to the balance that he is allowed to withdraw.
	 */
	private final StorageMap<PayableContract, BigInteger> balances = new StorageTreeMap<>();

	public @FromContract(PayableContract.class) GradualPonziWithBalance() {
		investors.add((PayableContract) caller());
	}

	public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "you must invest at least " + MINIMUM_INVESTMENT);
		BigInteger eachInvestorGets = amount.divide(BigInteger.valueOf(investors.size()));
		investors.stream().forEachOrdered(investor -> balances.update(investor, BigInteger.ZERO, eachInvestorGets::add));
		investors.add((PayableContract) caller());
	}

	public @FromContract(PayableContract.class) void withdraw() {
		PayableContract payee = (PayableContract) caller();
		payee.receive(balances.getOrDefault(payee, BigInteger.ZERO));
		balances.put(payee, BigInteger.ZERO);
	}
}