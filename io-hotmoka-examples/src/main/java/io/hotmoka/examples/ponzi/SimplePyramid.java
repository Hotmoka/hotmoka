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
import io.takamaka.code.util.StorageLinkedList;

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
public class SimplePyramid extends Contract {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(10_000L);
	private final StorageList<PayableContract> investors = new StorageLinkedList<>();
	private int previousLayerSize = 1;

	public @Payable @FromContract(PayableContract.class) SimplePyramid(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "you must invest at least " + MINIMUM_INVESTMENT);
		investors.add((PayableContract) caller());
	}

	public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "you must invest at least " + MINIMUM_INVESTMENT);
		investors.add((PayableContract) caller());

		if (investors.size() == previousLayerSize * 4 - 1) {
			// pay out previous layer: note that the current layer's size is even here
			investors.stream().skip(previousLayerSize - 1).limit(previousLayerSize).forEachOrdered(investor -> investor.receive(MINIMUM_INVESTMENT));
			// spread remaining money among all participants
			BigInteger eachInvestorGets = balance().divide(BigInteger.valueOf(investors.size()));
			investors.forEach(investor -> investor.receive(eachInvestorGets));
			previousLayerSize *= 2;
		}
	}
}