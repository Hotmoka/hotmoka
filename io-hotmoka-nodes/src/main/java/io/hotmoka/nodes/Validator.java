package io.hotmoka.nodes;

import java.security.PublicKey;

/**
 * The description of a validator of network of nodes.
 */

public final class Validator {

	/**
	 * The identifier of the validator, unique in the network.
	 */
	public final String id;

	/**
	 * The power of the validator, always positive.
	 */
	public final long power;

	/**
	 * The public key of the account, in the store of the node,
	 * that can be used to send money for paying the
	 * validation work of the validator.
	 */
	public final PublicKey publicKey;

	/**
	 * Builds the description of a validator.
	 * 
	 * @param id the identifier of the validator, unique in the network
	 * @param power the power of the validator
	 * @param publicKey the public key of the account, in the store of the node,
	 *                  that can be used to send money for paying the
	 *                  validation work of the validator
	 * @throws NullPointerException if {@code address} or {@code publicKey} is {@code null}
	 * @throws IllegalArgumentException if {@code power} is not positive
	 */
	public Validator(String id, long power, PublicKey publicKey) {
		if (id == null)
			throw new NullPointerException("the identifier of a validator cannot be null");

		if (power <= 0L)
			throw new IllegalArgumentException("the power of a validator cannot be negative");

		if (publicKey == null)
			throw new NullPointerException("the public key of the account bound to a validator cannot be null");

		this.id = id;
		this.power = power;
		this.publicKey = publicKey;
	}

	@Override
	public String toString() {
		return id + " with power " + power + " and public key " + publicKey;
	}
}