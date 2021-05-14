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

package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.takamaka.code.dao.SharedEntity.Offer;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;

/**
 * A shared entity where a shareholder must sell all its shares when it places an offer.
 *
 * @param <S> the type of the shareholders
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SharedEntityWithIntegralShares<S extends PayableContract, O extends Offer<S>> extends SimpleSharedEntity<S, O> {

	/**
	 * Creates a shared entity with the given set of shareholders and respective shares.
	 *
	 * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
	 * @param shares the initial shares of each initial shareholder. This must be as many as {@code shareholders}
	 */
	public SharedEntityWithIntegralShares(Stream<S> shareholders, Stream<BigInteger> shares) {
		super(shareholders, shares);
	}

	/**
	 * Creates a shared entity with the given set of shareholders and shares.
	 *
	 * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
	 * @param shares       the initial shares of each initial shareholder. This must have the same length as {@code shareholders}
	 */
	public SharedEntityWithIntegralShares(S[] shareholders, BigInteger[] shares) {
		super(Stream.of(shareholders), Stream.of(shares));
	}

	/**
	 * Creates a shared entity with one shareholder.
	 *
	 * @param shareholder  the initial shareholder
	 * @param share        the initial share of the initial shareholder
	 */
	public SharedEntityWithIntegralShares(S shareholder, BigInteger share) {
		super(Stream.of(shareholder), Stream.of(share));
	}

	/**
	 * Creates a shared entity with two shareholders.
	 *
	 * @param shareholder1 the first initial shareholder
	 * @param shareholder2 the second initial shareholder
	 * @param share1       the initial share of the first shareholder
	 * @param share2       the initial share of the second shareholder
	 */
	public SharedEntityWithIntegralShares(S shareholder1, S shareholder2, BigInteger share1, BigInteger share2) {
		super(Stream.of(shareholder1, shareholder2), Stream.of(share1, share2));
	}

	/**
	 * Creates a shared entity with three shareholders.
	 *
	 * @param shareholder1 the first initial shareholder
	 * @param shareholder2 the second initial shareholder
	 * @param shareholder3 the third initial shareholder
	 * @param share1       the initial share of the first shareholder
	 * @param share2       the initial share of the second shareholder
	 * @param share3       the initial share of the third shareholder
	 */
	public SharedEntityWithIntegralShares(S shareholder1, S shareholder2, S shareholder3, BigInteger share1, BigInteger share2, BigInteger share3) {
		super(Stream.of(shareholder1, shareholder2, shareholder3), Stream.of(share1, share2, share3));
	}

	/**
	 * Creates a shared entity with four shareholders.
	 *
	 * @param shareholder1 the first initial shareholder
	 * @param shareholder2 the second initial shareholder
	 * @param shareholder3 the third initial shareholder
	 * @param shareholder4 the fourth initial shareholder
	 * @param share1       the initial share of the first shareholder
	 * @param share2       the initial share of the second shareholder
	 * @param share3       the initial share of the third shareholder
	 * @param share4       the initial share of the fourth shareholder
	 */
	public SharedEntityWithIntegralShares(S shareholder1, S shareholder2, S shareholder3, S shareholder4, BigInteger share1, BigInteger share2, BigInteger share3, BigInteger share4) {
		super(Stream.of(shareholder1, shareholder2, shareholder3, shareholder4), Stream.of(share1, share2, share3, share4));
	}

	@Override
	public @FromContract(PayableContract.class) @Payable void place(BigInteger amount, O offer) {
		require(sharesOf(offer.seller).equals(offer.sharesOnSale), "the seller must sell its shares in full");
		super.place(amount, offer);
	}
}