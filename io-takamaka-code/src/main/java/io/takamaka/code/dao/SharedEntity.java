package io.takamaka.code.dao;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.lang.*;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageSet;
import io.takamaka.code.util.StorageSetView;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.StorageTreeSet;

/**
 * A shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * 
 * @param <O> the type of the offers of sale of shares for this entity
 */
public class SharedEntity<O extends SharedEntity.Offer> extends PayableContract {

	/**
	 * The shares of each shareholder. These are always positive.
	 */
	private final StorageTreeMap<PayableContract, BigInteger> shares = new StorageTreeMap<>();

	/**
	 * The set of offers of sale of shares.
	 */
	private final StorageSet<O> offers = new StorageTreeSet<>();

	/**
	 * A snapshot of the current shares.
	 */
	private StorageMapView<PayableContract, BigInteger> snapshotOfShares;

	/**
	 * A snapshot of the current offers.
	 */
	private StorageSetView<O> snapshotOfOffers;

	/**
	 * Creates a shared entity with the given set of shareholders and respective shares.
	 * 
	 * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
	 * @param shares the initial shares of each initial shareholder. This must have the same length as
	 *               {@code shareholders}
	 */
	public SharedEntity(PayableContract[] shareholders, BigInteger[] shares) {
		require(shareholders != null, "shareholders cannot be null");
		require(shares != null, "shares cannot be null");
		require(shareholders.length == shares.length, "shareholders and shares must have the same length");

		int pos = 0;
		for (PayableContract shareholder: shareholders) {
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
	public SharedEntity(PayableContract shareholder, BigInteger share) {
		this(new PayableContract[]{shareholder}, new BigInteger[]{share});
	}

	/**
     * Creates a shared entity with two shareholders.
     *
     * @param shareholder1 the first initial shareholder
     * @param shareholder2 the second initial shareholder
     * @param share1       the initial share of the first shareholder
     * @param share2       the initial share of the second shareholder
     */
    public SharedEntity(PayableContract shareholder1, PayableContract shareholder2, BigInteger share1, BigInteger share2) {
        this(new PayableContract[]{ shareholder1, shareholder2 }, new BigInteger[]{ share1, share2 });
    }

    /**
	 * Yields the offers existing at this moment. Note that some
	 * of these offers might be expired.
	 * 
	 * @return the offers
	 */
	public @View final StorageSetView<O> getOffers() {
		return snapshotOfOffers;
	}

	/**
	 * Yields the current shares, for each current shareholder.
	 * 
	 * @return the shares
	 */
	public @View final StorageMapView<PayableContract, BigInteger> getShares() {
		return snapshotOfShares;
	}

	/**
	 * Place an offer of sale of shares for this entity. By default, this method checks
	 * the offer, adds it to the current offers and issues an event. Subclasses may redefine.
	 * 
	 * @param amount the ticket payed to place the offer; this can be zero, but subclasses
	 *               may require differently
	 * @param offer the offer that is going to be placed
	 */
	public @FromContract(PayableContract.class) @Payable void place(BigInteger amount, O offer) {
		PayableContract seller = (PayableContract) caller();
		require(offer.seller == seller, "only the seller can place its own offer");
		require(shares.containsKey(seller), "the seller is not a shareholder");
		require(sharesOf(seller).subtract(sharesOnSale(seller)).compareTo(offer.sharesOnSale) >= 0, "the seller has not enough shares to sell");
		cleanUpOffers(null);
		offers.add(offer);
		snapshotOfOffers = offers.snapshot();
		event(new OfferPlaced(offer));
	}

	/**
	 * Called whenever an offer is being accepted. By default, this method
	 * deletes the offer and transfers the shares. Subclasses may redefine,
	 * for instance to impose constraints on the offer being accepted.
	 * 
	 * @param amount the ticket payed for accepting the offer; this must at least
	 *               pay for the cost of {@code offer}, but subclasses may require
	 *               to pay an extra ticket
	 * @param offer the accepted offer
	 */
	public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount, O offer) {
		require(offers.contains(offer), "unknown offer");
		require(isOngoing(offer), "the sale offer is not ongoing anymore");
		require(offer.cost.compareTo(amount) <= 0, "not enough money to accept the offer");
		PayableContract buyer = (PayableContract) caller();
		cleanUpOffers(offer);
		removeShares(offer.seller, offer.sharesOnSale);
		addShares(buyer, offer.sharesOnSale);
		offer.seller.receive(offer.cost);
		snapshotOfShares = shares.snapshot();
		event(new OfferAccepted(buyer, offer));
	}

	/**
	 * Yields the current shares of the given shareholder.
	 * 
	 * @param shareholder the shareholder
	 * @return the shares. Yields zero if {@code shareholder} is currently not a shareholder
	 */
	private BigInteger sharesOf(PayableContract shareholder) {
		return shares.getOrDefault(shareholder, ZERO);
	}

	/**
	 * Deletes offers that have expired.
	 * 
	 * @param offerToRemove an offer whose first occurrence must be removed
	 */
	private void cleanUpOffers(O offerToRemove) {
		offers.stream()
			.filter(offer -> offer == offerToRemove || !isOngoing(offer))
			.forEachOrdered(offers::remove);
	}

	private void addShares(PayableContract shareholder, BigInteger added) {
		shares.update(shareholder,
			() -> {
				event(new ShareholderAdded(shareholder));
				return ZERO;
			},
			added::add);
	}

	private void removeShares(PayableContract shareholder, BigInteger removed) {
		shares.update(shareholder, shares -> shares.subtract(removed));
		if (shares.get(shareholder).signum() == 0) {
			shares.remove(shareholder);
			event(new ShareholderRemoved(shareholder));
		}
	}

	private BigInteger sharesOnSale(PayableContract seller) {
		return offers.stream()
			.filter(offer -> offer.seller == seller && isOngoing(offer))
			.map(offer -> offer.sharesOnSale)
			.reduce(ZERO, BigInteger::add);
	}

	/**
	 * Determines if the given offer is ongoing, that is, it is not yet expired.
	 * 
	 * @param offer the offer to check
	 * @return true if and only if that condition holds
	 */
	private boolean isOngoing(O offer) {
		return now() <= offer.expiration;
	}

	/**
	 * The description of a sale offer of shares.
	 */
	@Exported
	public static class Offer extends Storage {

		/**
		 * The seller.
		 */
		public final PayableContract seller;

		/**
		 * The number of shares on sale, always positive.
		 */
		public final BigInteger sharesOnSale;

		/**
		 * The cost, always non-negative.
		 */
		public final BigInteger cost;

		/**
		 * The expiration of the sale, in milliseconds from 1/1/1970.
		 */
		public final long expiration;

		/**
		 * Create the description of a sale offer.
		 * 
		 * @param sharesOnSale the shares on sale, positive
		 * @param cost the cost, non-negative
		 * @param duration the duration of validity of the offer, in milliseconds from now, always non-negative
		 */
		public @FromContract(PayableContract.class) Offer(BigInteger sharesOnSale, BigInteger cost, long duration) {
			require(sharesOnSale != null && sharesOnSale.signum() > 0, "the shares on sale must be a positive big integer");
			require(cost != null && cost.signum() >= 0, "the cost must be a non-negative big integer");
			require(duration >= 0, "the duration cannot be negative");

			this.seller = (PayableContract) caller();
			this.sharesOnSale = sharesOnSale;
			this.cost = cost;
			this.expiration = now() + duration;
		}
	}

	public final static class OfferPlaced extends Event {
		public final Offer offer;

		private @FromContract OfferPlaced(Offer offer) {
			this.offer = offer;
		}
	}

	public final static class OfferAccepted extends Event {
		public final Offer offer;
		public final PayableContract buyer;

		/**
		 * Creates the event.
		 * 
		 * @param buyer the buyer of the offered shares
		 * @param offer the offer being accepted
		 */
		private @FromContract OfferAccepted(PayableContract buyer, Offer offer) {
			this.buyer = buyer;
			this.offer = offer;
		}
	}

	public final static class ShareholderAdded extends Event {
		public final PayableContract shareholder;
	
		private @FromContract ShareholderAdded(PayableContract shareholder) {
			this.shareholder = shareholder;
		}
	}

	public final static class ShareholderRemoved extends Event {
		public final PayableContract shareholder;

		private @FromContract ShareholderRemoved(PayableContract shareholder) {
			this.shareholder = shareholder;
		}
	}
}