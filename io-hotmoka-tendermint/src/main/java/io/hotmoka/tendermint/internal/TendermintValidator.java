package io.hotmoka.tendermint.internal;

import java.io.IOException;
import java.io.ObjectInputStream;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;

/**
 * The description of a validator of a Tendermint network.
 */
final class TendermintValidator extends Marshallable {

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
	 * @param address the address of the validator, unique in the network
	 * @param power the power of the validator
	 * @throws NullPointerException if {@code address} is {@code null}
	 * @throws IllegalArgumentException if {@code power} is not positive
	 */
	public TendermintValidator(String address, long power) {
		if (address == null)
			throw new NullPointerException("the address of a validator cannot be null");

		if (power <= 0L)
			throw new IllegalArgumentException("the power of a validator cannot be negative");

		this.address = address;
		this.power = power;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeUTF(address);
		context.oos.writeLong(power);
	}

	/**
	 * Factory method that unmarshals the description of a validator from the given stream.
	 * 
	 * @param ois the stream
	 * @return the description of the validator
	 * @throws IOException if the description of the validator could not be unmarshalled
	 * @throws ClassNotFoundException if the description of the validator could not be unmarshalled
	 */
	public static TendermintValidator from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new TendermintValidator(ois.readUTF(), ois.readLong());
	}

	@Override
	public String toString() {
		return address + " with power " + power;
	}
}