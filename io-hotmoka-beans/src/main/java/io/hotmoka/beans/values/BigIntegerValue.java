package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A big integer stored in blockchain.
 */
@Immutable
public final class BigIntegerValue extends StorageValue {
	static final byte SELECTOR = 6;

	/**
	 * The big integer.
	 */
	public final BigInteger value;

	/**
	 * Builds a big integer that can be stored in blockchain.
	 * 
	 * @param value the big integer
	 */
	public BigIntegerValue(BigInteger value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BigIntegerValue && ((BigIntegerValue) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((BigIntegerValue) other).value);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		marshal(value, oos);
	}
}