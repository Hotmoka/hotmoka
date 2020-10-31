package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.Iterator;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.Takamaka;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageSet;

/**
 * The shared entity. Shareholders hold, sell and buy shares.
 * By iterating on a shared entity, one gets its current shareholders.
 * 
 * @param <Shareholder> the type of the shareholders of the entity
 */
public class SharedEntity<Shareholder extends Storage> extends Storage implements Iterable<Shareholder> {

	/**
	 * The shares of each shareholder. These are always positive.
	 */
	private final StorageMap<Shareholder, BigInteger> shares = new StorageMap<>();

	/**
	 * The shares currently on sale, for each shareholder. These are always non-negative.
	 * This information could be computed from the {@code offers}, but this field,
	 * although redundant, simplifies the implementation.
	 */
	private final StorageMap<Shareholder, BigInteger> sharesOnSale = new StorageMap<>();

	/**
	 * The set of offers of sale of shares.
	 */
	private final StorageSet<OnSale> offers = new StorageSet<>();

	/**
	 * Creates a shared entity with the given set of shareholders and respective shares.
	 * 
	 * @param shareholders the initial shareholders; if there are repetitions, their shares are merged
	 * @param shares the initial shares of each initial shareholder. This must have the same length as
	 *               {@code shareholders}
	 */
	public SharedEntity(Shareholder[] shareholders, BigInteger[] shares) {
		require(shareholders != null, "shareholders cannot be null");
		require(shares != null, "shares cannot be null");
		require(shareholders.length == shares.length, "shareholders and shares must have the same length");

		int pos = 0;
		for (Shareholder shareholder: shareholders) {
			BigInteger added = shares[pos];
			require(added != null && added.signum() > 0, "shares must be positive big integers");
			addSharesTo(shareholder, added);
			this.sharesOnSale.put(shareholder, ZERO);
		}
	}

	/**
	 * Yields the current shares of the given shareholder.
	 * 
	 * @param shareholder the shareholder
	 * @return the shares. Yields zero if {@code shareholder} is currently not a shareholder
	 */
	public @View final BigInteger sharesOf(Shareholder shareholder) {
		BigInteger result = shares.get(shareholder);
		return result != null ? result : ZERO;
	}

	@Override
	public final Iterator<Shareholder> iterator() {
		return shares.keyList().iterator();
	}

	private void addSharesTo(Shareholder shareholder, BigInteger added) {
		BigInteger total = this.shares.get(shareholder);
		total = total == null ? added : total.add(added);
		this.shares.put(shareholder, total);
	}

	/**
	 * The description of a sale.
	 */
	public class OnSale extends Storage {

		/**
		 * The seller.
		 */
		public final Shareholder seller;

		/**
		 * The number of shares on sale, always positive.
		 */
		public final BigInteger shares;

		/**
		 * The cost, always non-negative.
		 */
		public final BigInteger cost;

		/**
		 * The expiration of the sale, in milliseconds from 1/1/1970, exactly
		 * as for {@link System#currentTimeMillis()}.
		 */
		public final long expiration;

		/**
		 * Creates the description of a sale.
		 * 
		 * @param seller the seller
		 * @param shares the shares on sale, positive
		 * @param cost the cost, non-negative
		 * @param expiration the expiration, in milliseconds from 1/1/1970, exactly
		 *                   like {@link System#currentTimeMillis()}
		 */
		public OnSale(Shareholder seller, BigInteger shares, BigInteger cost, long expiration) {
			Takamaka.require(seller != null, "seller cannot be null");
			Takamaka.require(shares != null && shares.signum() > 0, "the shares must be a positive big integer");
			Takamaka.require(cost != null && cost.signum() >= 0, "the cost must be a non-negative big integer");
			Takamaka.require(expiration >= 0, "the expiration cannot be negative");

			this.seller = seller;
			this.shares = shares;
			this.cost = cost;
			this.expiration = expiration;
		}
	}
}