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
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * A contract for a pyramid investment scheme:
 * each layer is twice as large as the previous layer.
 * Each layer receives its investment back when the next layer fills.
 * The leftover money is then distributed among all participants.
 * 
 * This example is translated from a Solidity contract shown
 * in "Building Games with Ethereum Smart Contracts", by Iyer and Dannen,
 * page 155, Apress 2018.
 */
public class SimplePyramidWithBalance extends Contract {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(10_000L);
	private final StorageList<PayableContract> investors = new StorageLinkedList<>();
	private int previousLayerSize = 1;
	private final StorageMap<PayableContract, BigInteger> balances = new StorageTreeMap<>();
	private BigInteger pyramidBalance;

	public @Payable @FromContract(PayableContract.class) SimplePyramidWithBalance(BigInteger amount) {
		require(BigIntegerSupport.compareTo(amount, MINIMUM_INVESTMENT) >= 0, () -> StringSupport.concat("you must invest at least ", MINIMUM_INVESTMENT));
		investors.add((PayableContract) caller());
		pyramidBalance = amount;
	}

	public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
		require(BigIntegerSupport.compareTo(amount, MINIMUM_INVESTMENT) >= 0, () -> StringSupport.concat("you must invest at least ", MINIMUM_INVESTMENT));
		pyramidBalance = BigIntegerSupport.add(pyramidBalance, amount);
		investors.add((PayableContract) caller());

		if (investors.size() == previousLayerSize * 4 - 1) {
			// pay out previous layer: note that currentLayer's size is even here
			// pay out previous layer: note that the current layer's size is even here
			var it = investors.iterator();
			for (int i = 1; i <= previousLayerSize - 1 && it.hasNext(); i++)
				it.next(); // we skip previousLayerSize - 1 investors
			for (int i = 1; i <= previousLayerSize && it.hasNext(); i++)
				balances.update(it.next(), ZERO, bi -> BigIntegerSupport.add(bi, MINIMUM_INVESTMENT));
			// spread remaining money among all participants
			BigInteger eachInvestorGets = BigIntegerSupport.divide(BigIntegerSupport.subtract(pyramidBalance, BigIntegerSupport.multiply(MINIMUM_INVESTMENT, BigInteger.valueOf(previousLayerSize))), BigInteger.valueOf(investors.size()));
			investors.forEach(investor -> balances.update(investor, ZERO, bi -> BigIntegerSupport.add(bi, eachInvestorGets)));
			pyramidBalance = ZERO;
			previousLayerSize *= 2;
		}
	}

	public @FromContract(PayableContract.class) void withdraw() {
		((PayableContract) caller()).receive(balances.getOrDefault(caller(), ZERO));
		balances.remove(caller());
	}
}