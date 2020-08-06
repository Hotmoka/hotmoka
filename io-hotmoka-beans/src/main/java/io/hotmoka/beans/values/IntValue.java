package io.hotmoka.beans.values;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;

/**
 * An {@code int} value stored in blockchain.
 */
@Immutable
public final class IntValue extends StorageValue {
	static final byte SELECTOR = 13;

	/**
	 * The value.
	 */
	public final int value;

	/**
	 * Builds an {@code int} value.
	 * 
	 * @param value the value
	 */
	public IntValue(int value) {
		this.value = value;
	}

	/**
	 * Builds a {@code long} value.
	 * 
	 * @param value the value
	 */
	public IntValue(BigInteger value) {
		this.value = value.intValue();
		if (!BigInteger.valueOf(this.value).equals(value))
			throw new IllegalArgumentException("value is too big for an int");
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof IntValue && ((IntValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Integer.compare(value, ((IntValue) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (value >= 0 && value < 255 - SELECTOR)
			context.oos.writeByte(SELECTOR + 1 + value);
		else {
			context.oos.writeByte(SELECTOR);
			context.oos.writeInt(value);
		}
	}
}