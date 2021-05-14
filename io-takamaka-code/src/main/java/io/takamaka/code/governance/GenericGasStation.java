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

package io.takamaka.code.governance;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.function.Function;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * A generic implementation of a contract that keeps track of the price of gas.
 * 
 * @param <V> the type of the validators in the manifest of this gas station
 */
public class GenericGasStation<V extends Validator> extends Contract implements GasStation<V> {

	/**
	 * The manifest of the node.
	 */
	private final Manifest<V> manifest;

	/**
	 * The maximal gas limit that can be offered by a transaction request.
	 * Requests with higher gas limits will be rejected.
	 */
	private final BigInteger maxGasPerTransaction;

	/**
	 * True if and only if the node ignores the minimum gas price.
	 * Hence requests that specify a lower gas price
	 * than the current gas price of the node are executed anyway.
	 * This is mainly useful for testing. It defaults to false.
	 */
	private final boolean ignoresGasPrice;

	/**
	 * The units of gas that are aimed to be rewarded at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 */
	private final BigInteger targetGasAtReward;

	private final BigInteger maxOblivion = BigInteger.valueOf(MAX_OBLIVION);

	/**
	 * How quick the gas consumed at previous rewards is forgotten
	 * for the determination of the gas price:
	 * 0 means never, {@link #MAX_OBLIVION} means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 */
	private final BigInteger oblivion;

	/**
	 * The inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 0 means 0%, 100,000 means 1%,
	 * 10,000,000 means 100%, 20,000,000 means 200% and so on.
	 * Inflation can be negative. For instance, -30,000 means -0.3%.
	 * This defaults to 10,000 (that is, inflation is 0.1% by default).
	 */
	private final long inflation;

	private final BigInteger COMPLEMENT_OF_OBLIVION;

	private final BigInteger DIVISOR;

	/**
	 * The gas consumed in the past, a weight of the last rewards.
	 */
	private BigInteger pastGasConsumedWeighted;

	private BigInteger remainder;

	/**
	 * The current gas price, that is, the amount of coins necessary to buy
	 * a unit of gas. This is always strictly positive.
	 */
	private BigInteger gasPrice;

	/**
	 * Builds an object that keeps track of the price of the gas.
	 * 
	 * @param manifest the manifest of the node
	 * @param maxGasPerTransaction the maximal gas limit that can be offered by a transaction request.
	 *                             Requests with higher gas limits will be rejected
	 * @param ignoresGasPrice true if and only if the node ignores the minimum gas price.
	 *                        Hence requests that specify a lower gas price
	 *                        than the current gas price of the node are executed anyway.
	 *                        This is mainly useful for testing
	 * @param targetGasAtReward the units of gas that we aim as average at each reward.
	 *                          If the actual reward is smaller, the price of gas must decrease.
	 *                          If it is larger, the price of gas must increase
	 * @param oblivion how quick the gas consumed at previous rewards is forgotten
	 *                 in the determination of the gas price:
	 *                 0 means never, {@link #MAX_OBLIVION} means immediately.
	 *                 Hence a smaller level means that the latest rewards are heavier
	 *                 in the determination of the gas price
	 * @param inflation the inflation applied to the gas consumed by transactions before it gets sent
	 *                  as reward to the validators. 0 means 0%, 100,000 means 1%,
	 *                  10,000,000 means 100%, 20,000,000 means 200% and so on.
	 *                  Inflation can be negative. For instance, -30,000 means -0.3%
	 */
	GenericGasStation(Manifest<V> manifest, BigInteger maxGasPerTransaction, boolean ignoresGasPrice,
			BigInteger targetGasAtReward, long oblivion, long inflation) {

		this.manifest = manifest;
		this.maxGasPerTransaction = maxGasPerTransaction;
		this.ignoresGasPrice = ignoresGasPrice;
		this.targetGasAtReward = targetGasAtReward;
		this.oblivion = BigInteger.valueOf(oblivion);
		this.inflation = inflation;
		this.COMPLEMENT_OF_OBLIVION = maxOblivion.subtract(this.oblivion);
		this.pastGasConsumedWeighted = targetGasAtReward.multiply(COMPLEMENT_OF_OBLIVION);
		this.DIVISOR = targetGasAtReward.multiply(maxOblivion);
		this.gasPrice = BigInteger.valueOf(100L); // initial attempt
		this.remainder = ZERO;
	}

	@Override
	public @FromContract void takeNoteOfGasConsumedDuringLastReward(BigInteger gasConsumed) {
		require(caller() == manifest.validators, "only the validators can call this method");
		require(gasConsumed.signum() >= 0, "the gas consumed cannot be negative");

		// we give OBLIVION weight to the latest gas consumed and the complement to the past
		BigInteger weighted =
			gasConsumed.multiply(oblivion)
			.add(pastGasConsumedWeighted);

		pastGasConsumedWeighted = weighted.multiply(COMPLEMENT_OF_OBLIVION).divide(maxOblivion);

		BigInteger previousGasPrice = gasPrice;
		BigInteger[] division = previousGasPrice.multiply(weighted)
			.add(remainder)
			.divideAndRemainder(DIVISOR);

		gasPrice = division[0];
		remainder = division[1];

		// the gas price must always be positive
		if (gasPrice.signum() == 0)
			gasPrice = ONE;

		if (!gasPrice.equals(previousGasPrice))
			event(new GasPriceUpdate(gasPrice));
	}

	@Override
	public final @View BigInteger getMaxGasPerTransaction() {
		return maxGasPerTransaction;
	}

	@Override
	public final @View BigInteger getGasPrice() {
		return gasPrice;
	}

	@Override
	public final @View boolean ignoresGasPrice() {
		return ignoresGasPrice;
	}

	@Override
	public final @View BigInteger getTargetGasAtReward() {
		return targetGasAtReward;
	}

	@Override
	public @View long getOblivion() {
		return oblivion.longValue();
	}

	@Override
	public @View long getInflation() {
		return inflation;
	}

	@Exported
	public static class Builder<V extends Validator> extends Storage implements Function<Manifest<V>, GasStation<V>> {
		private final BigInteger maxGasPerTransaction;
		private final boolean ignoresGasPrice;
		private final BigInteger targetGasAtReward;
		private final long oblivion;
		private final long inflation;

		/**
		 * @param maxGasPerTransaction the maximal gas limit that can be offered by a transaction request.
		 *                             Requests with higher gas limits will be rejected
		 * @param ignoresGasPrice true if and only if the node ignores the minimum gas price.
		 *                        Hence requests that specify a lower gas price
		 *                        than the current gas price of the node are executed anyway.
		 *                        This is mainly useful for testing
		 * @param targetGasAtReward the units of gas that we aim as average at each reward.
		 *                          If the actual reward is smaller, the price of gas must decrease.
		 *                          If it is larger, the price of gas must increase
		 * @param oblivion how quick the gas consumed at previous rewards is forgotten
		 *                 in the determination of the gas price:
		 *                 0 means never, {@link #MAX_OBLIVION} means immediately.
		 *                 Hence a smaller level means that the latest rewards are heavier
		 *                 in the determination of the gas price
		 * @param inflation the inflation applied to the gas consumed by transactions before it gets sent
		 *                  as reward to the validators. 0 means 0%, 100,000 means 1%,
		 *                  10,000,000 means 100%, 20,000,000 means 200% and so on.
		 *                  Inflation can be negative. For instance, -30,000 means -0.3%
		 */
		public Builder(BigInteger maxGasPerTransaction, boolean ignoresGasPrice, BigInteger targetGasAtReward, long oblivion, long inflation) {
			this.maxGasPerTransaction = maxGasPerTransaction;
			this.ignoresGasPrice = ignoresGasPrice;
			this.targetGasAtReward = targetGasAtReward;
			this.oblivion = oblivion;
			this.inflation = inflation;
		}

		@Override
		public GasStation<V> apply(Manifest<V> manifest) {
			return new GenericGasStation<>(manifest, maxGasPerTransaction, ignoresGasPrice, targetGasAtReward, oblivion, inflation);
		}
	}
}