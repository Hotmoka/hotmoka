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

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.takamaka.code.dao.SharedEntity.Offer;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
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
public class SimpleSharedEntity<S extends PayableContract, O extends Offer<S>> extends PayableContract implements SharedEntity<S, O> {

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
	 * @param shares the initial shares of each initial shareholder. This must be as many as {@code shareholders}
	 */
	public SimpleSharedEntity(Stream<S> shareholders, Stream<BigInteger> shares) {
		require(shareholders != null, "shareholders cannot be null");
		require(shares != null, "shares cannot be null");
	
		List<S> shareholdersAsList = shareholders.collect(Collectors.toList());
		List<BigInteger> sharesAsList = shares.collect(Collectors.toList());
	
		require(shareholdersAsList.size() == sharesAsList.size(), "shareholders and shares must have the same length");
	
		Iterator<BigInteger> it = sharesAsList.iterator();
		for (S shareholder: shareholdersAsList) {
			require(shareholder != null, "shareholders cannot be null");
			BigInteger added = it.next();
			require(added != null && added.signum() > 0, "shares must be positive big integers");
			addShares(shareholder, added);
		}
	
		this.snapshotOfShares = this.shares.snapshot();
		this.snapshotOfOffers = offers.snapshot();
	}

	/**
	 * Creates a shared entity with the given set of shareholders and respective shares.
	 *
	 * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
	 * @param shares the initial shares of each initial shareholder. This must have the same length as {@code shareholders}
	 */
	public SimpleSharedEntity(S[] shareholders, BigInteger[] shares) {
		this(Stream.of(shareholders), Stream.of(shares));
	}

	/**
	 * Creates a shared entity with one shareholder.
	 *
	 * @param shareholder the initial shareholder
	 * @param share the initial share of the initial shareholder
	 */
	public SimpleSharedEntity(S shareholder, BigInteger share) {
		this(Stream.of(shareholder), Stream.of(share));
	}

	/**
     * Creates a shared entity with two shareholders.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     */
    public SimpleSharedEntity(S shareholder1, S shareholder2, BigInteger share1, BigInteger share2) {
    	this(Stream.of(shareholder1, shareholder2), Stream.of(share1, share2));
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
    public SimpleSharedEntity(S shareholder1, S shareholder2, S shareholder3, BigInteger share1, BigInteger share2, BigInteger share3) {
    	this(Stream.of(shareholder1, shareholder2, shareholder3), Stream.of(share1, share2, share3));
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
    public SimpleSharedEntity(S shareholder1, S shareholder2, S shareholder3, S shareholder4, BigInteger share1, BigInteger share2, BigInteger share3, BigInteger share4) {
    	this(Stream.of(shareholder1, shareholder2, shareholder3, shareholder4), Stream.of(share1, share2, share3, share4));
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
	public final Stream<S> getShareholders() {
		return snapshotOfShares.keys();
	}

    @Override
	public final @View BigInteger sharesOf(S shareholder) {
		return shares.getOrDefault(shareholder, ZERO);
	}

    @Override
	public final @View BigInteger sharesOnSaleOf(S shareholder) {
		return offers.stream()
			.filter(offer -> offer.seller == shareholder && offer.isOngoing())
			.map(offer -> offer.sharesOnSale)
			.reduce(ZERO, BigInteger::add);
	}

    @Override
	public @FromContract(PayableContract.class) @Payable void place(BigInteger amount, O offer) {
		require(offer.seller == caller(), "only the seller can place its own offer");
		require(shares.containsKey(offer.seller), "the seller is not a shareholder");
		require(sharesOf(offer.seller).subtract(sharesOnSaleOf(offer.seller)).compareTo(offer.sharesOnSale) >= 0, "the seller has not enough shares to sell");
		cleanUpOffers(null);
		offers.add(offer);
		snapshotOfOffers = offers.snapshot();
		event(new OfferPlaced<>(offer));
	}

    @Override
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, S buyer, O offer) {
    	require(caller() == buyer, "only the future owner can buy the shares");
		require(offers.contains(offer), "unknown offer");
		require(offer.isOngoing(), "the sale offer is not ongoing anymore");
		require(offer.cost.compareTo(amount) <= 0, "not enough money to accept the offer");
		cleanUpOffers(offer);
		removeShares(offer.seller, offer.sharesOnSale);
		addShares(buyer, offer.sharesOnSale);
		offer.seller.receive(offer.cost);
		snapshotOfShares = shares.snapshot();
		snapshotOfOffers = offers.snapshot();
		event(new OfferAccepted<>(buyer, offer));
	}

	@Override
	public SharedEntityView<S> view() {

		@Exported
		class SharedEntityViewImpl extends Storage implements SharedEntityView<S> {

			@Override @View
			public StorageMapView<S, BigInteger> getShares() {
				return SimpleSharedEntity.this.getShares();
			}

			@Override
			public Stream<S> getShareholders() {
				return SimpleSharedEntity.this.getShareholders();
			}

			@Override @View
			public boolean isShareholder(Object who) {
				return SimpleSharedEntity.this.isShareholder(who);
			}

			@Override @View
			public BigInteger sharesOf(S shareholder) {
				return SimpleSharedEntity.this.sharesOf(shareholder);
			}

			@Override
			public SharedEntityView<S> snapshot() {
				return SimpleSharedEntity.this.snapshot();
			}
		}

		return new SharedEntityViewImpl();
	}

	@Override
	public final SharedEntityView<S> snapshot() {

		@Exported
		class SharedEntitySnapshotImpl extends Storage implements SharedEntityView<S> {

			/**
			 * Saves the shares at the time of creation of the snapshot.
			 */
			private final StorageMapView<S, BigInteger> snapshotOfShares = SimpleSharedEntity.this.snapshotOfShares;

			@Override @View
			public StorageMapView<S, BigInteger> getShares() {
				return snapshotOfShares;
			}

			@Override
			public Stream<S> getShareholders() {
				return snapshotOfShares.keys();
			}

			@Override @View
			public boolean isShareholder(Object who) {
				return snapshotOfShares.containsKey(who);
			}

			@Override @View
			public BigInteger sharesOf(S shareholder) {
				return snapshotOfShares.getOrDefault(shareholder, ZERO);
			}

			@Override
			public SharedEntityView<S> snapshot() {
				return this;
			}
		}

		return new SharedEntitySnapshotImpl();
	}

	/**
	 * Deletes offers that have expired.
	 * 
	 * @param offerToRemove an offer whose first occurrence must be removed
	 */
	private void cleanUpOffers(O offerToRemove) {
		offers.stream()
			.filter(offer -> offer == offerToRemove || !offer.isOngoing())
			.forEachOrdered(offers::remove);
	}

	private void addShares(S shareholder, BigInteger added) {
		shares.update(shareholder,
			() -> {
				event(new ShareholderAdded<>(shareholder));
				return ZERO;
			},
			added::add);
	}

	private void removeShares(S shareholder, BigInteger removed) {
		shares.update(shareholder, shares -> shares.subtract(removed));
		if (shares.get(shareholder).signum() == 0) {
			shares.remove(shareholder);
			event(new ShareholderRemoved<>(shareholder));
		}
	}
}