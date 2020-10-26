package io.takamaka.code.system;

import io.takamaka.code.lang.ExternallyOwnedAccount;

/**
 * The validator of a network of nodes. It can be used to
 * collect money when transactions get validated.
 */
public final class Validator extends ExternallyOwnedAccount {

	/**
	 * The identifier of the validator, unique in the network.
	 */
	public final String id;

	/**
	 * The power of the validator, always positive.
	 */
	public final long power;

	/**
	 * Creates a validator. It starts as an externally owned account with no funds.
	 * 
	 * @param id the identifier of the validator, unique in the network; this can be
	 *           anything, as long as it does not contain spaces; it is case-insensitive
	 *           and will be stored in lower-case
	 * @param power the power of the validator
	 * @param publicKey the Base64-encoded public key of the account
	 * @throws NullPointerException if {@code id} or {@code publicKey} is null
	 */
	public Validator(String id, long power, String publicKey) {
		super(publicKey);

		if (id == null)
			throw new NullPointerException("the identifier of a validator cannot be null");

		if (power <= 0L)
			throw new IllegalArgumentException("the power of a validator cannot be negative");

		if (id.contains(" "))
			throw new IllegalArgumentException("spaces are not allowed in a validator identifier");

		this.id = id.toLowerCase();
		this.power = power;
	}

	@Override
	public String toString() {
		return id + " with power " + power;
	}
}