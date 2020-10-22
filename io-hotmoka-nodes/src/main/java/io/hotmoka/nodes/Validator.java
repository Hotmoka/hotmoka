package io.hotmoka.nodes;

import java.io.IOException;
import java.io.ObjectInputStream;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;

/**
 * The description of a validator of a network on nodes.
 */
public final class Validator extends Marshallable {

	/**
	 * The identifier of the validator, unique in the network.
	 */
	public final String id;

	/**
	 * The power of the validator, positive.
	 */
	public final long power;

	/**
	 * Builds the description of a validator.
	 * 
	 * @param id the identifier, unique in the network
	 * @param power the power of the validator
	 * @throws IllegalArgumentException if {@code power} is not positive
	 */
	public Validator(String id, long power) {
		if (power <= 0L)
			throw new IllegalArgumentException("the power of a validator must be positive");

		this.id = id;
		this.power = power;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeUTF(id);
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
	public static Validator from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new Validator(ois.readUTF(), ois.readLong());
	}

	@Override
	public String toString() {
		return id + " with power " + power;
	}
}