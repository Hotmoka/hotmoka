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

package io.hotmoka.tendermint.internal;

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
	public final String publicKey;

	/**
	 * The type of public key of the validator.
	 */
	public final String publicKeyType;

	/**
	 * Builds the description of a validator.
	 * 
	 * @param address the address of the validator; this cannot contain spaces
	 * @param power the power of the validator
	 * @param publicKey the public key of the validator; this cannot contain spaces
	 * @param publicKeyType; the public key type of the validator; this cannot contain spaces
	 * @throws NullPointerException if {@code address} is {@code null}
	 * @throws IllegalArgumentException if {@code power} is not positive or any of {@code address},
	 *                                  {@code publicKey} or {@code publicKeyType} contain spaces
	 */
	public TendermintValidator(String address, long power, String publicKey, String publicKeyType) {
		if (address == null)
			throw new NullPointerException("the address of a validator cannot be null");

		if (address.contains(" "))
			throw new IllegalArgumentException("the address of a validator cannot contain spaces");

		if (power <= 0L)
			throw new IllegalArgumentException("the power of a validator cannot be negative");

		if (publicKey.contains(" "))
			throw new IllegalArgumentException("the public key of a validator cannot contain spaces");

		if (publicKeyType.contains(" "))
			throw new IllegalArgumentException("the public key type of a validator cannot contain spaces");	

		this.address = address.toUpperCase();
		this.power = power;
		this.publicKey = publicKey;
		this.publicKeyType = publicKeyType;
	}

	@Override
	public String toString() {
		return "Tendermint validator " + address + ", power = " + power + ", publicKey = " + publicKey + " of type " + publicKeyType;
	}
}