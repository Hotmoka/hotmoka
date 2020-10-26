package io.hotmoka.tendermint;

/**
 * The description of a validator of a Tendermint network.
 */
public final class TendermintValidator {

	/**
	 * The address of the validator, unique in the network.
	 */
	public final String address;

	/**
	 * The power of the validator, always positive.
	 */
	public final long power;

	/**
	 * Builds the description of a validator.
	 * 
	 * @param address the address of the validator, unique in the network;
	 *                this cannot contain spaces
	 * @param power the power of the validator
	 * @throws NullPointerException if {@code address} is {@code null}
	 * @throws IllegalArgumentException if {@code power} is not positive or {@code address} contains spaces
	 */
	public TendermintValidator(String address, long power) {
		if (address == null)
			throw new NullPointerException("the address of a validator cannot be null");

		if (address.contains(" "))
			throw new IllegalArgumentException("the address of a validator cannot contain spaces");

		if (power <= 0L)
			throw new IllegalArgumentException("the power of a validator cannot be negative");

		this.address = address;
		this.power = power;
	}

	@Override
	public String toString() {
		return address + " with power " + power;
	}
}