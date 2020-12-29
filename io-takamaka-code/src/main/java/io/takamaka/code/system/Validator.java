package io.takamaka.code.system;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.View;

/**
 * The validator of a network of nodes. It can be used to
 * collect money when transactions get validated. It is an account
 * with an identity string, that is used to identify validators that
 * must be rewarded or punished at each validation step.
 * The identity must be derived from the public key of the validator,
 * hence it can coincide with that key or can be an abstraction of it.
 */
public class Validator extends ExternallyOwnedAccount {

	/**
	 * Creates a validator with no initial funds.
	 * 
	 * @param publicKey the Base64-encoded public key of the validator
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	public Validator(String publicKey) {
		super(publicKey);
	}

	/**
	 * Yields the identifier of the validator. By default, this is the
	 * public key of the account, but subclasses may redefine.
	 * 
	 * @return the identifier of the validator. This must be derived from the
	 *         public key in a sufficiently distinctive way
	 */
	public @View String id() {
		return publicKey();
	}
}