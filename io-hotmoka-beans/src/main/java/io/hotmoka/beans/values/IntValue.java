package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.ObjectOutputStream;

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
	public void into(ObjectOutputStream oos) throws IOException {
		if (value >= 0 && value < 255 - SELECTOR)
			oos.writeByte(SELECTOR + 1 + value);
		else {
			oos.writeByte(SELECTOR);
			oos.writeInt(value);
		}
	}
}