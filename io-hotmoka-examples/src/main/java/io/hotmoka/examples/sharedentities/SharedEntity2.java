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
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageSetView;

/**
 * A shared entity. Shareholders hold, sell and buy shares of a shared entity.
 * It uses a generic type for the shareholders, but does not provide precise
 * type information to the {@link #accept(BigInteger, Offer)} method, whose
 * implementation will need to perform an unsafe cast to the shareholders' type.
 * 
 * @param <S> the type of the shareholders
 * @param <O> the type of the offers of sale of shares for this entity
 */
public interface SharedEntity2<S extends PayableContract, O extends SharedEntity2.Offer<S>> {

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
	 * @param offer the accepted offer
	 */
	@FromContract(PayableContract.class) @Payable void accept(BigInteger amount, O offer);

	/**
	 * The description of a sale offer of shares.
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
		 * Create the description of a sale offer.
		 * 
		 * @param sharesOnSale the shares on sale, positive
		 * @param cost the cost, non-negative
		 * @param duration the duration of validity of the offer, in milliseconds from now, always non-negative
		 */
		@SuppressWarnings("unchecked")
		public @FromContract(PayableContract.class) Offer(BigInteger sharesOnSale, BigInteger cost, long duration) {
			require(sharesOnSale != null && sharesOnSale.signum() > 0, "the shares on sale must be a positive big integer");
			require(cost != null && cost.signum() >= 0, "the cost must be a non-negative big integer");
			require(duration >= 0, "the duration cannot be negative");

			this.seller = (S) caller();  // unsafe cast: this allows anybody to create an offer, also who is not an S
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

	final class OfferPlaced<S extends PayableContract> extends Event {
		public final Offer<S> offer;

		protected @FromContract OfferPlaced(Offer<S> offer) {
			this.offer = offer;
		}
	}

	final class OfferAccepted<S extends PayableContract> extends Event {
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

	final class ShareholderAdded<S extends PayableContract> extends Event {
		public final S shareholder;
	
		protected @FromContract ShareholderAdded(S shareholder) {
			this.shareholder = shareholder;
		}
	}

	final class ShareholderRemoved<S extends PayableContract> extends Event {
		public final S shareholder;

		protected @FromContract ShareholderRemoved(S shareholder) {
			this.shareholder = shareholder;
		}
	}
}