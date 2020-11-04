package io.takamaka.code.system;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.Takamaka;
import io.takamaka.code.lang.View;

/**
 * The validator of a network of nodes. It can be used to
 * collect money when transactions get validated.
 */
public final class Validator extends ExternallyOwnedAccount {

	/**
	 * The identifier of the validator, unique in the network.
	 * This must be fixed for a given secret.
	 */
	public final String id;

	/**
	 * The secret, once revealed.
	 */
	private String secret;

	/**
	 * The type of the secret, once revealed.
	 */
	private String type;

	/**
	 * Creates a validator. It starts as an externally owned account with no funds.
	 * 
	 * @param id the identifier of the validator, unique in the network; this can be
	 *           anything, as long as it does not contain spaces; it is case-insensitive
	 *           and will be stored in lower-case
	 * @param publicKey the Base64-encoded public key of the Takamaka account
	 * @throws NullPointerException if {@code id} or {@code publicKey} is null
	 */
	public Validator(String id, String publicKey) {
		super(publicKey);

		if (id == null)
			throw new NullPointerException("the identifier of a validator cannot be null");

		if (id.contains(" "))
			throw new IllegalArgumentException("spaces are not allowed in a validator identifier");

		this.id = id.toLowerCase();
	}

	public Validator(String id, String publicKey, String type, String secret) {
		this(id, publicKey);

		this.type = type;
		this.secret = secret;
	}

	public @FromContract void reveal(String secret, String type) {
		Takamaka.require(caller() == this, "only the validator itself can set its secret");

		if (secret == null)
			throw new NullPointerException("the secret cannot be null");

		if (type == null)
			throw new NullPointerException("the type cannot be null");

		this.type = type;
		this.secret = secret;
	}

	public @FromContract boolean isRevealed() {
		return secret != null;
	}

	public @View String getSecret() {
		return secret;
	}

	public @View String getType() {
		return type;
	}

	@Override
	public String toString() {
		return type + ' '  + secret;
	}
}