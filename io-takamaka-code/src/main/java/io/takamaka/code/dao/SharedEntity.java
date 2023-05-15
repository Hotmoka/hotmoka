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

import static io.takamaka.code.lang.Takamaka.now;
import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageSetView;

/**
 * A shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * 
 * @param <S> the type of the shareholders
 * @param <O> the type of the offers of sale of shares for this entity
 */
public interface SharedEntity<S extends PayableContract, O extends SharedEntity.Offer<S>> extends SharedEntityView<S> {

    /**
	 * Yields the offers existing at this moment. Note that some
	 * of these offers might be expired.
	 * 
	 * @return the offers
	 */
	@View StorageSetView<O> getOffers();

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
	 * Yields a view of this entity. The view reflects the shares in this entity:
	 * any future modification of this entity will be seen also through the view.
	 * A view is always {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a view of this entity
	 */
	SharedEntityView<S> view();

	/**
	 * The description of a sale offer of shares.
	 * 
	 * @param <S> the type of the seller contract
	 */
	@Exported
	class Offer<S extends PayableContract> extends Storage {

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
		 * The only buyer that can buy this offer. This is null if everybody can buy this offer.
		 */
		public final S buyer;

		/**
		 * Create a sale offer.
		 * 
		 * @param seller the seller of the shares; this must coincide with the caller of the constructor
		 * @param sharesOnSale the shares on sale, positive
		 * @param cost the cost, non-negative
		 * @param duration the duration of validity of the offer, in milliseconds from now, always non-negative
		 */
		public Offer(S seller, BigInteger sharesOnSale, BigInteger cost, long duration) {
			require(sharesOnSale != null && sharesOnSale.signum() > 0, "the shares on sale must be a positive big integer");
			require(cost != null && cost.signum() >= 0, "the cost must be a non-negative big integer");
			require(duration >= 0, "the duration cannot be negative");

			this.seller = seller;
			this.sharesOnSale = sharesOnSale;
			this.cost = cost;
			this.expiration = now() + duration;
			this.buyer = null;
		}

		/**
		 * Create a reserved sale offer.
		 * 
		 * @param seller the seller of the shares; this must coincide with the caller of the constructor
		 * @param sharesOnSale the shares on sale, positive
		 * @param cost the cost, non-negative
		 * @param duration the duration of validity of the offer, in milliseconds from now, always non-negative
		 * @param buyer the only buyer allowed for this offer
		 */
		public Offer(S seller, BigInteger sharesOnSale, BigInteger cost, long duration, S buyer) {
			require(sharesOnSale != null && sharesOnSale.signum() > 0, "the shares on sale must be a positive big integer");
			require(cost != null && cost.signum() >= 0, "the cost must be a non-negative big integer");
			require(duration >= 0, "the duration cannot be negative");

			this.seller = seller;
			this.sharesOnSale = sharesOnSale;
			this.cost = cost;
			this.expiration = now() + duration;
			this.buyer = buyer;
		}
		
		/**
		 * Determines if this offer is ongoing, that is, it is not yet expired.
		 * 
		 * @return true if and only if that condition holds
		 */
		public @View boolean isOngoing() {
			return now() <= expiration;
		}

		/**
		 * Yields the only buyer allowed for this offer.
		 * 
		 * @return the buyer, or null if everybody can buy this offer
		 */
		public @View S getBuyer() {
			return buyer;
		}

		/**
		 * Yields the seller of this offer.
		 * 
		 * @return the seller of this offer
		 */
		public @View S getSeller() {
			return seller;
		}

		/**
		 * Yields the amount of shares on sale with this offer.
		 * 
		 * @return the amount of shares on sale with this offer
		 */
		public @View BigInteger getSharesOnSale() {
			return sharesOnSale;
		}

		/**
		 * Yields the cost of this offer.
		 * 
		 * @return the cost of this offer
		 */
		public @View BigInteger getCost() {
			return cost;
		}

		/**
		 * Yields the expiration time of this offer, in milliseconds from 1/1/1970.
		 * 
		 * @return the expiration time of this offer
		 */
		public @View long getExpiration() {
			return expiration;
		}
	}

	/**
	 * An event triggered when a offer of sale has been placed.
	 *
	 * @param <S> the type of the seller contract
	 */
	final class OfferPlaced<S extends PayableContract> extends Event {

		/**
		 * The offer.
		 */
		public final Offer<S> offer;

		/**
		 * Creates the event.
		 * 
		 * @param offer the offer
		 */
		protected @FromContract OfferPlaced(Offer<S> offer) {
			this.offer = offer;
		}
	}

	/**
	 * An event triggered when a offer of sale has been accepted.
	 *
	 * @param <S> the type of the seller contract
	 */
	final class OfferAccepted<S extends PayableContract> extends Event {

		/**
		 * The offer.
		 */
		public final Offer<S> offer;

		/**
		 * The buyer.
		 */
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

	/**
	 * An event triggered when a new shareholder is added.
	 *
	 * @param <S> the type of the shareholder
	 */
	final class ShareholderAdded<S extends PayableContract> extends Event {

		/**
		 * The new shareholder.
		 */
		public final S shareholder;

		/**
		 * Creates the event.
		 * 
		 * @param shareholder the new shareholder
		 */
		protected @FromContract ShareholderAdded(S shareholder) {
			this.shareholder = shareholder;
		}
	}

	/**
	 * An event triggered when a shareholder has been removed.
	 *
	 * @param <S> the type of the shareholder
	 */
	final class ShareholderRemoved<S extends PayableContract> extends Event {

		/**
		 * The removed shareholder.
		 */
		public final S shareholder;

		/**
		 * Creates the event.
		 * 
		 * @param shareholder the removed shareholder
		 */
		protected @FromContract ShareholderRemoved(S shareholder) {
			this.shareholder = shareholder;
		}
	}
}