package io.takamaka.code.dao;

import io.takamaka.code.lang.*;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageSetView;

import java.math.BigInteger;
import java.util.stream.Stream;

import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;

/**
 * A shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * 
 * @param <O> the type of the offers of sale of shares for this entity
 */
public interface SharedEntity3<O extends SharedEntity3.Offer<S>, S extends PayableContract> {

    /**
	 * Yields the offers existing at this moment. Note that some
	 * of these offers might be expired.
	 * 
	 * @return the offers
	 */
	@View StorageSetView<O> getOffers();

	/**
	 * Yields the current shares, for each current shareholder.
	 * 
	 * @return the shares
	 */
	@View StorageMapView<S, BigInteger> getShares();

	/**
	 * Yields the shareholders.
	 * 
	 * @return the shareholders
	 */
	Stream<S> getShareholders();

	/**
	 * Determine if the given object is a shareholder of this entity.
	 * 
	 * @param who the potential shareholder
	 * @return true if and only if {@code who} is a shareholder of this entity
	 */
	@View boolean isShareholder(Object who);

	/**
	 * Yields the current shares of the given shareholder.
	 * 
	 * @param shareholder the shareholder
	 * @return the shares. Yields zero if {@code shareholder} is currently not a shareholder
	 */
	@View BigInteger sharesOf(S shareholder);

	/**
	 * Yields the total amount of shares that the given shareholder has currently on sale.
	 * This only includes sell offers that are ongoing at the moment.
	 * 
	 * @param shareholder the seller
	 * @return the total amount of shares
	 */
	@View BigInteger sharesOnSaleOf(S shareholder);

	/**
	 * Place an offer of sale of shares for this entity. This method checks
	 * the offer, adds it to the current offers and issues an event.
	 * 
	 * @param amount the ticket payed to place the offer; implementations may allow zero for this
	 * @param offer the offer that is going to be placed
	 */
	@FromContract(PayableContract.class) @Payable void place(BigInteger amount, O offer);

	/**
	 * Called whenever an offer is being accepted. This method
	 * deletes the offer and transfers the shares. Implementations may
	 * impose constraints on the offer being accepted.
	 * 
	 * @param amount the ticket payed for accepting the offer; this must at least
	 *               pay for the cost of {@code offer}, but implementations may require
	 *               to pay an extra ticket
	 * @param buyer the buyer of the shares; this must coincide with the caller of the method
	 * @param offer the accepted offer
	 */
	@FromContract(PayableContract.class) @Payable void accept(BigInteger amount, S buyer, O offer);

	/**
	 * The description of a sale offer of shares.
	 */
	@Exported
	public static class Offer<S extends PayableContract> extends Storage {

		/**
		 * The seller.
		 */
		public final S seller;

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
		 * @param seller the seller of the shares; this must coincide with the caller of the constructor
		 * @param sharesOnSale the shares on sale, positive
		 * @param cost the cost, non-negative
		 * @param duration the duration of validity of the offer, in milliseconds from now, always non-negative
		 */
		public @FromContract(PayableContract.class) Offer(S seller, BigInteger sharesOnSale, BigInteger cost, long duration) {
			require(caller() == seller, "only the owner can sell its shares");
			require(sharesOnSale != null && sharesOnSale.signum() > 0, "the shares on sale must be a positive big integer");
			require(cost != null && cost.signum() >= 0, "the cost must be a non-negative big integer");
			require(duration >= 0, "the duration cannot be negative");

			this.seller = seller;
			this.sharesOnSale = sharesOnSale;
			this.cost = cost;
			this.expiration = now() + duration;
		}

		/**
		 * Determines if this offer is ongoing, that is, it is not yet expired.
		 * 
		 * @return true if and only if that condition holds
		 */
		public @View boolean isOngoing() {
			return now() <= expiration;
		}
	}

	public final static class OfferPlaced<S extends PayableContract> extends Event {
		public final Offer<S> offer;

		protected @FromContract OfferPlaced(Offer<S> offer) {
			this.offer = offer;
		}
	}

	public final static class OfferAccepted<S extends PayableContract> extends Event {
		public final Offer<S> offer;
		public final S buyer;

		/**
		 * Creates the event.
		 * 
		 * @param buyer the buyer of the offered shares
		 * @param offer the offer being accepted
		 */
		protected @FromContract OfferAccepted(S buyer, Offer<S> offer) {
			this.buyer = buyer;
			this.offer = offer;
		}
	}

	public final static class ShareholderAdded<S extends PayableContract> extends Event {
		public final S shareholder;
	
		protected @FromContract ShareholderAdded(S shareholder) {
			this.shareholder = shareholder;
		}
	}

	public final static class ShareholderRemoved<S extends PayableContract> extends Event {
		public final S shareholder;

		protected @FromContract ShareholderRemoved(S shareholder) {
			this.shareholder = shareholder;
		}
	}
}