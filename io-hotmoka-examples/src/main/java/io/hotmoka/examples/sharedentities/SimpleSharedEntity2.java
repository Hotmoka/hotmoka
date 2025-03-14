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

package io.hotmoka.examples.sharedentities;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageSet;
import io.takamaka.code.util.StorageSetView;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.StorageTreeSet;

/**
 * A simple implementation of a shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * Selling and buying shares do not require to pay a ticket.
 * 
 * @param <S> the type of the shareholders
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SimpleSharedEntity2<S extends PayableContract, O extends SharedEntity2.Offer<S>> extends PayableContract implements SharedEntity2<S, O> {

	/**
	 * The shares of each shareholder. These are always positive.
	 */
	private final StorageTreeMap<S, BigInteger> shares = new StorageTreeMap<>();

	/**
	 * The set of offers of sale of shares.
	 */
	private final StorageSet<O> offers = new StorageTreeSet<>();

	/**
	 * A snapshot of the current shares.
	 */
	private StorageMapView<S, BigInteger> snapshotOfShares;

	/**
	 * A snapshot of the current offers.
	 */
	private StorageSetView<O> snapshotOfOffers;

	/**
	 * Creates a shared entity with the given set of shareholders and respective shares.
	 *
	 * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
	 * @param shares the initial shares of each initial shareholder. This must have the same length as {@code shareholders}
	 */
	public SimpleSharedEntity2(S[] shareholders, BigInteger[] shares) {
		require(shareholders != null, "shareholders cannot be null");
		require(shares != null, "shares cannot be null");
		require(shareholders.length == shares.length, "shareholders and shares must have the same length");

		int pos = 0;
		for (S shareholder: shareholders) {
			require(shareholder != null, "shareholders cannot be null");
			BigInteger added = shares[pos++];
			require(added != null && added.signum() > 0, "shares must be positive big integers");
			addShares(shareholder, added);
		}

		this.snapshotOfShares = this.shares.snapshot();
		this.snapshotOfOffers = offers.snapshot();
	}

	/**
	 * Creates a shared entity with one shareholder.
	 *
	 * @param shareholder the initial shareholder
	 * @param share the initial share of the initial shareholder
	 */
	public SimpleSharedEntity2(S shareholder, BigInteger share) {
		require(shareholder != null, "shareholders cannot be null");
    	require(share != null && share.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder, share);

    	this.snapshotOfShares = this.shares.snapshot();
		this.snapshotOfOffers = offers.snapshot();
	}

	/**
     * Creates a shared entity with two shareholders.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     */
    public SimpleSharedEntity2(S shareholder1, S shareholder2, BigInteger share1, BigInteger share2) {
    	require(shareholder1 != null, "shareholders cannot be null");
    	require(share1 != null && share1.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder1, share1);
    	require(shareholder2 != null, "shareholders cannot be null");
    	require(share2 != null && share2.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder2, share2);

    	this.snapshotOfShares = this.shares.snapshot();
		this.snapshotOfOffers = offers.snapshot();
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
    public SimpleSharedEntity2(S shareholder1, S shareholder2, S shareholder3, BigInteger share1, BigInteger share2, BigInteger share3) {
    	require(shareholder1 != null, "shareholders cannot be null");
    	require(share1 != null && share1.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder1, share1);
    	require(shareholder2 != null, "shareholders cannot be null");
    	require(share2 != null && share2.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder2, share2);
    	require(shareholder3 != null, "shareholders cannot be null");
    	require(share3 != null && share3.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder3, share3);

    	this.snapshotOfShares = this.shares.snapshot();
		this.snapshotOfOffers = offers.snapshot();
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
    public SimpleSharedEntity2(S shareholder1, S shareholder2, S shareholder3, S shareholder4, BigInteger share1, BigInteger share2, BigInteger share3, BigInteger share4) {
    	require(shareholder1 != null, "shareholders cannot be null");
    	require(share1 != null && share1.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder1, share1);
    	require(shareholder2 != null, "shareholders cannot be null");
    	require(share2 != null && share2.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder2, share2);
    	require(shareholder3 != null, "shareholders cannot be null");
    	require(share3 != null && share3.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder3, share3);
    	require(shareholder4 != null, "shareholders cannot be null");
    	require(share4 != null && share4.signum() > 0, "shares must be positive big integers");
    	addShares(shareholder4, share4);

    	this.snapshotOfShares = this.shares.snapshot();
		this.snapshotOfOffers = offers.snapshot();
    }

    @Override
	public @View final StorageSetView<O> getOffers() {
		return snapshotOfOffers;
	}

    @Override
	public @View final StorageMapView<S, BigInteger> getShares() {
		return snapshotOfShares;
	}

    @Override
    public @View final boolean isShareholder(Object who) {
    	return snapshotOfShares.containsKey(who);
    }

    @Override
	public final @View BigInteger sharesOf(S shareholder) {
		return shares.getOrDefault(shareholder, ZERO);
	}

    @Override
	public final @View BigInteger sharesOnSaleOf(S shareholder) {
    	BigInteger sum = ZERO;

    	for (O offer: offers)
    		if (offer.seller == shareholder && offer.isOngoing())
    			sum = BigIntegerSupport.add(sum, offer.sharesOnSale);

    	return sum;
	}

    @Override
	public @FromContract(PayableContract.class) @Payable void place(BigInteger amount, O offer) {
		require(offer.seller == caller(), "only the seller can place its own offer");
		require(shares.containsKey(offer.seller), "the seller is not a shareholder");
		require(BigIntegerSupport.compareTo(BigIntegerSupport.subtract(sharesOf(offer.seller), sharesOnSaleOf(offer.seller)), offer.sharesOnSale) >= 0, "the seller has not enough shares to sell");
		cleanUpOffers(null);
		offers.add(offer);
		snapshotOfOffers = offers.snapshot();
		event(new OfferPlaced<>(offer));
	}

    @Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, O offer) {
		require(offers.contains(offer), "unknown offer");
		require(offer.isOngoing(), "the sale offer is not ongoing anymore");
		require(BigIntegerSupport.compareTo(offer.cost, amount) <= 0, "not enough money to accept the offer");
		@SuppressWarnings("unchecked")
		S buyer = (S) caller(); // unsafe cast: this allows anybody to become a shareholder, also who is not an S
		cleanUpOffers(offer);
		removeShares(offer.seller, offer.sharesOnSale);
		addShares(buyer, offer.sharesOnSale);
		offer.seller.receive(offer.cost);
		snapshotOfShares = shares.snapshot();
		event(new OfferAccepted<>(buyer, offer));
	}

	/**
	 * Deletes offers that have expired.
	 * 
	 * @param offerToRemove an offer whose first occurrence must be removed
	 */
	private void cleanUpOffers(O offerToRemove) {
		for (O offer: offers)
			if (offer == offerToRemove || !offer.isOngoing())
				offers.remove(offer);
	}

	private void addShares(S shareholder, BigInteger added) {
		shares.update(shareholder,
			() -> {
				event(new ShareholderAdded<>(shareholder));
				return ZERO;
			},
			bi -> BigIntegerSupport.add(bi, added));
	}

	private void removeShares(S shareholder, BigInteger removed) {
		shares.update(shareholder, shares -> BigIntegerSupport.subtract(shares, removed));
		if (shares.get(shareholder).signum() == 0) {
			shares.remove(shareholder);
			event(new ShareholderRemoved<>(shareholder));
		}
	}
}