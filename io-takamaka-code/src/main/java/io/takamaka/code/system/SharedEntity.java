package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageSet;

/**
 * A shared entity. Shareholders hold, sell and buy shares of a shared entity.
 */
public class SharedEntity extends PayableContract {

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
	 * Yields the currently ongoing offers.
	 * 
	 * @return the offers that are still ongoing
	 */
	/*public StorageSet<Offer> computeOngoingOffers() {
		cleanUpOffers();
		return offers;
	}*/

	/**
	 * Called whenever an offer is being placed. By default, this method
	 * adds the offer to the current offers and issues an event. Subclasses may redefine.
	 * 
	 * @param amount the ticket payed to place the offer
	 */
	protected @FromContract(Offer.class) @Payable void placeOffer(BigInteger amount) {
		Offer offer = (Offer) caller();
		require(offer.getSharedEntity() == this, "cannot place an offer of another shared entity");
		cleanUpOffers();
		offers.add(offer);
		event(new OfferPlaced(offer));
	}

	/**
	 * Called whenever an offer is being accepted. By default, this method
	 * deletes the offer and transfers the shares. Subclasses may redefine,
	 * for instance to impose constraints on the offer being accepted.
	 * 
	 * @param amount the ticket payed for accepting the offer
	 * @param buyer the buyer of the offered shares
	 */
	protected @FromContract(Offer.class) @Payable void acceptOffer(BigInteger amount, PayableContract buyer) {
		Offer offer = (Offer) caller();
		require(offers.contains(offer), "the offer has been already accepted");
		require(offer.getSharedEntity() == this, "cannot accept an offer of another shared entity");
		offers.remove(offer);
		removeShares(offer.seller, offer.sharesOnSale);
		addShares(buyer, offer.sharesOnSale);
		event(new OfferAccepted(buyer, offer));
	}

	/**
	 * Deletes offers that have expired.
	 */
	private void cleanUpOffers() {
		List<Offer> expired = offers.stream().filter(offer -> !offer.isOngoing()).collect(Collectors.toList());
		expired.forEach(offers::remove);
	}

	private void addShares(PayableContract shareholder, BigInteger added) {
		if (shares.get(shareholder) == null)
			new ShareholderAdded(shareholder);

		shares.update(shareholder, ZERO, added::add);
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
			.filter(offer -> offer.seller == seller)
			.filter(Offer::isOngoing)
			.map(offer -> offer.sharesOnSale)
			.reduce(ZERO, BigInteger::add);
	}

	/**
	 * The description of a sale offer of shares.
	 */
	public class Offer extends Contract {

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
		 * Create a sale offer. Creation of a new offer requires to pay a ticket,
		 * that gets forwarded to the shared entity of this offer.
		 * This implementation allows a zero ticket, but subclasses may redefine.
		 * 
		 * @param amount the ticket payed to place a new offer
		 * @param seller the seller
		 * @param sharesOnSale the shares on sale, positive
		 * @param cost the cost, non-negative
		 * @param duration the duration of validity of the offer, in milliseconds from now, always non-negative
		 */
		public @FromContract(PayableContract.class) @Payable Offer(BigInteger amount, PayableContract seller, BigInteger sharesOnSale, BigInteger cost, long duration) {
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

			placeOffer(amount);
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
		 * buyer contract, in exchange of the cost of the sale plus a ticket. This implementation
		 * allows a zero ticket, but subclasses may redefine.
		 * 
		 * @param amount the ticket to pay to accept the offer
		 */
		public final @FromContract(PayableContract.class) @Payable void accept(BigInteger amount) {
			require(isOngoing(), "the sale offer is not ongoing anymore");
			require(cost.compareTo(amount) <= 0, "not enough money to accept the offer");
			PayableContract buyer = (PayableContract) caller();
			seller.receive(cost);
			acceptOffer(amount.subtract(cost), buyer);
		}

		/**
		 * Determines if this offer is ongoing, that is, it is not yet expired.
		 * 
		 * @return true if and only if that condition holds
		 */
		private boolean isOngoing() {
			return now() <= expiration;
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

	public final static class ShareholderRemoved extends Event {
		public final PayableContract shareholder;

		private @FromContract ShareholderRemoved(PayableContract shareholder) {
			this.shareholder = shareholder;
		}
	}

	public final static class ShareholderAdded extends Event {
		public final PayableContract shareholder;

		private @FromContract ShareholderAdded(PayableContract shareholder) {
			this.shareholder = shareholder;
		}
	}
}