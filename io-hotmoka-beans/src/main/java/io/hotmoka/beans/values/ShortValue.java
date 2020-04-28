package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A {@code short} value stored in blockchain.
 */
@Immutable
public final class ShortValue extends StorageValue {
	static final byte SELECTOR = 9;

	/**
	 * The value.
	 */
	public final short value;

	/**
	 * Builds a {@code short} value.
	 * 
	 * @param value the value
	 */
	public ShortValue(short value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Short.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ShortValue && ((ShortValue) other).value == value;
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
			return Short.compare(value, ((ShortValue) other).value);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		oos.writeShort(value);
	}
}