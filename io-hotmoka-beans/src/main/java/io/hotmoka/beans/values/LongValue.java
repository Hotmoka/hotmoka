package io.hotmoka.beans.values;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;

/**
 * A {@code long} value stored in blockchain.
 */
@Immutable
public final class LongValue extends StorageValue {
	static final byte SELECTOR = 7;

	/**
	 * The value.
	 */
	public final long value;

	/**
	 * Builds a {@code long} value.
	 * 
	 * @param value the value
	 */
	public LongValue(long value) {
		this.value = value;
	}

	/**
	 * Builds a {@code long} value.
	 * 
	 * @param value the value
	 */
	public LongValue(BigInteger value) {
		this.value = value.longValue();
		if (!BigInteger.valueOf(this.value).equals(value))
			throw new IllegalArgumentException("value is too big for a long");
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LongValue && ((LongValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Long.compare(value, ((LongValue) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeLong(value);
	}
}