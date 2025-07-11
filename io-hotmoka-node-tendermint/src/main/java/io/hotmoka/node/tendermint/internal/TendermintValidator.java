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

package io.hotmoka.node.tendermint.internal;

import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.crypto.Base64;

/**
 * The description of a validator of a Tendermint network.
 */
public final class TendermintValidator {

	/**
	 * The address of the validator.
	 */
	public final String address;

	/**
	 * The power of the validator, always positive.
	 */
	public final long power;

	/**
	 * The public key of the validator.
	 */
	private final String publicKey;

	/**
	 * The public key of the validator, encoded as an array of bytes.
	 */
	private final byte[] publicKeyEncoded;

	/**
	 * The type of public key of the validator.
	 */
	public final String publicKeyType;

	/**
	 * Builds the description of a validator.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param address the address of the validator; this cannot contain spaces
	 * @param power the power of the validator
	 * @param publicKey the public key of the validator; this cannot contain spaces
	 * @param publicKeyType; the public key type of the validator; this cannot contain spaces
	 * @param ifIllegal the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	public <E extends Exception> TendermintValidator(String address, long power, String publicKey, String publicKeyType, ExceptionSupplierFromMessage<? extends E> ifIllegal) throws E {
		this.address = Objects.requireNonNull(address, "address cannot be null", ifIllegal).toUpperCase();
		this.power = power;
		this.publicKey = Objects.requireNonNull(publicKey, "publicKey cannot be null", ifIllegal);
		this.publicKeyEncoded = Base64.fromBase64String(publicKey, ifIllegal);
		this.publicKeyType = Objects.requireNonNull(publicKeyType, "publicKeyType cannot be null", ifIllegal);

		if (address.contains(" "))
			throw ifIllegal.apply("the address of a validator cannot contain spaces");

		if (power <= 0L)
			throw ifIllegal.apply("The power of a validator cannot be negative");

		if (publicKey.contains(" "))
			ifIllegal.apply("The public key of a validator cannot contain spaces");

		if (publicKeyType.contains(" "))
			ifIllegal.apply("The public key type of a validator cannot contain spaces");	
	}

	/**
	 * Yields the public key of the validator, as an array of bytes.
	 * 
	 * @return the public key of the validator, as an array of bytes
	 */
	public byte[] getPubliKeyEncoded() {
		return publicKeyEncoded.clone();
	}

	@Override
	public String toString() {
		return "Tendermint validator " + address + ", power = " + power + ", publicKey = " + publicKey + " of type " + publicKeyType;
	}
}