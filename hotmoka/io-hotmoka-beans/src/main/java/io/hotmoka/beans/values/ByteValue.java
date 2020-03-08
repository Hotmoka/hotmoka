package io.hotmoka.beans.values;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A {@code byte} value stored in blockchain.
 */
@Immutable
public final class ByteValue implements StorageValue {

	private static final long serialVersionUID = -387404205334801866L;

	/**
	 * The value.
	 */
	public final byte value;

	/**
	 * Builds a {@code byte} value.
	 * 
	 * @param value the value
	 */
	public ByteValue(byte value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Byte.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ByteValue && ((ByteValue) other).value == value;
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
			return Byte.compare(value, ((ByteValue) other).value);
	}
}