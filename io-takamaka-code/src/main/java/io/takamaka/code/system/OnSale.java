package io.takamaka.code.system;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.Takamaka;

/**
 * The description of an object on sale.
 * 
 * @param <Seller> the type of the seller
 * @param <Item> the type of the object on sale
 */
public class OnSale<Seller extends Storage, Item> extends Storage {

	/**
	 * The seller.
	 */
	public final Seller seller;

	/**
	 * The item on sale.
	 */
	public final Item item;

	/**
	 * The cost, always non-negative.
	 */
	public final BigInteger cost;

	/**
	 * The expiration, in milliseconds from 1/1/1970, exactly
	 * like {@link System#currentTimeMillis()}.
	 */
	public final long expiration;

	/**
	 * Creates the description of an object on sale.
	 * 
	 * @param seller the seller
	 * @param item the object on sale
	 * @param cost the cost
	 * @param expiration the expiration, in millisenconds from 1/1/1970, exactly
	 *                   like {@link System#currentTimeMillis()}
	 */
	public OnSale(Seller seller, Item item, BigInteger cost, long expiration) {
		Takamaka.require(seller != null, "seller cannot be null");
		Takamaka.require(item != null, "the item cannot be null");
		Takamaka.require(cost != null && cost.signum() >= 0, "the cost must be a non-negative big integer");
		Takamaka.require(expiration >= 0, "the expiration cannot be negative");

		this.seller = seller;
		this.item = item;
		this.cost = cost;
		this.expiration = expiration;
	}
}