package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Immutable;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageSet;

/**
 * A shared entity. Shareholders hold, sell and buy shares of a shared entity.
 */
public class SharedEntity extends Contract {

	/**
	 * The shares of each shareholder. These are always positive.
	 */
	private final StorageMap<PayableContract, BigInteger> shares = new StorageMap<>();

	/**
	 * The set of offers of sale of shares.
	 */
	private final StorageSet<Offer> offers = new StorageSet<>();

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
	}

	/**
	 * Yields the current shares of the given shareholder.
	 * 
	 * @param shareholder the shareholder
	 * @return the shares. Yields zero if {@code shareholder} is currently not a shareholder
	 */
	public @View final BigInteger sharesOf(PayableContract shareholder) {
		return shares.getOrDefault(shareholder, ZERO);
	}

	/**
	 * Places an offer to sell shares of this entity.
	 * 
	 * @param amount the amount payed to place the offer. There is currently no constraint on this,
	 *               but subclasses may redefine the
	 * @param sharesOnSale the shares on sale, positive
	 * @param cost the cost of sale, non-negative
	 * @param duration the maximal duration of the sale, in milliseconds from now, always non-negative;
	 *                 after that time, the offer will expire
	 * @return the offer of sale
	 */
	public final @FromContract(PayableContract.class) @Payable Offer placeOffer(BigInteger amount, BigInteger sharesOnSale, BigInteger cost, long duration) {
		PayableContract seller = (PayableContract) caller();
		Offer offer = new Offer(seller, sharesOnSale, cost, duration);
		onPlacing(offer);
		offers.add(offer);
		event(new OfferPlaced(offer));
		return offer;
	}

	/**
	 * Called whenever an offer is being placed. By default, this method
	 * does not do anything. Subclasses may redefine to impose
	 * constraints on the offer and throw an exception if they do not hold,
	 * so that the offer gets rejected.
	 * 
	 * @param offer the offer that is being placed
	 */
	protected void onPlacing(Offer offer) {
	}

	/**
	 * Called whenever an offer is being accepted. By default, this method
	 * does not do anything. Subclasses may redefine to impose
	 * constraints on the offer being accepted and throw an exception if they
	 * do not hold, so that the acceptance gets rejected.
	 * 
	 * @param buyer the buyer of the offered shares
	 * @param payed the amount payed for buying the shares
	 * @param offer the offer being accepted
	 */
	protected void onAccepting(PayableContract buyer, BigInteger payed, Offer offer) {
	}

	private void generateAcceptanceEvent(PayableContract buyer, BigInteger payed, Offer offer) {
		event(new OfferAccepted(buyer, payed, offer));
	}

	private void addShares(PayableContract shareholder, BigInteger added) {
		shares.update(shareholder, ZERO, added::add);
	}

	private void removeShares(PayableContract shareholder, BigInteger removed) {
		shares.update(shareholder, shares -> shares.subtract(removed));
		if (ZERO.equals(shares.get(shareholder)))
			shares.remove(shareholder);
	}

	private BigInteger sharesOnSale(PayableContract seller) {
		return offers.stream()
			.filter(offer -> offer.seller == seller)
			.filter(Offer::isOngoing)
			.map(offer -> offer.sharesOnSale)
			.reduce(ZERO, BigInteger::add);
	}

	/**
	 * The description of a sale offer of shares.
	 */
	public final class Offer extends Contract {

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
		 * Create a sale offer.
		 * 
		 * @param seller the seller
		 * @param sharesOnSale the shares on sale, positive
		 * @param cost the cost, non-negative
		 * @param duration the duration, in milliseconds from now, always non-negative
		 */
		private Offer(PayableContract seller, BigInteger sharesOnSale, BigInteger cost, long duration) {
			require(seller != null, "the seller cannot be null");
			require(shares.contains(seller), "the seller is not a shareholder");
			require(sharesOnSale != null && sharesOnSale.signum() > 0, "the shares on sale must be a positive big integer");
			require(sharesOf(seller).subtract(sharesOnSale(seller)).compareTo(sharesOnSale) >= 0, "the seller has not enough shares to sell");
			require(cost != null && cost.signum() >= 0, "the cost must be a non-negative big integer");
			require(duration >= 0, "the duration cannot be negative");

			this.seller = seller;
			this.sharesOnSale = sharesOnSale;
			this.cost = cost;
			this.expiration = now() + duration;
		}

		/**
		 * Yields the shared entity whose shares are offered by this sale.
		 * 
		 * @return the shared entity
		 */
		public final @View SharedEntity getSharedEntity() {
			return SharedEntity.this;
		}

		/**
		 * Accepts an offer of sale of shares. The shares pass from the seller to the calling
		 * buyer contract, in exchange of the cost of the sale.
		 * 
		 * @param amount the money sent to pay for the offer
		 */
		public @FromContract(PayableContract.class) @Payable void accept(BigInteger amount) {
			require(isOngoing(), "the sale offer is not ongoing anymore");
			require(cost.compareTo(amount) <= 0, "not enough money to accept the offer");
			PayableContract buyer = (PayableContract) caller();
			onAccepting(buyer, amount, this);

			removeShares(seller, sharesOnSale);
			addShares(buyer, sharesOnSale);
			offers.remove(this);
			seller.receive(cost);
			generateAcceptanceEvent(buyer, amount, this);
		}

		/**
		 * Determines if this offer is ongoing, that is, is not yet expired and not yet accepted.
		 * 
		 * @return true if and only if that condition holds
		 */
		private boolean isOngoing() {
			return now() <= expiration && offers.contains(this);
		}
	}

	public final static @Immutable class OfferPlaced extends Event {
		private final Offer offer;

		private @FromContract OfferPlaced(Offer offer) {
			this.offer = offer;
		}

		public @View Offer getOffer() {
			return offer;
		}
	}

	public final static @Immutable class OfferAccepted extends Event {
		private final Offer offer;
		private final PayableContract buyer;
		private final BigInteger payed;

		/**
		 * Creates the event.
		 * 
		 * @param buyer the buyer of the offered shares
		 * @param payed the amount payed for buying the shares
		 * @param offer the offer being accepted
		 */
		private @FromContract OfferAccepted(PayableContract buyer, BigInteger payed, Offer offer) {
			this.buyer = buyer;
			this.payed = payed;
			this.offer = offer;
		}

		public @View Offer getOffer() {
			return offer;
		}

		public @View PayableContract getBuyer() {
			return buyer;
		}

		public @View BigInteger getPayed() {
			return payed;
		}
	}
}