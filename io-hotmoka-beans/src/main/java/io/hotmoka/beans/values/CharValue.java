package io.hotmoka.beans.values;

import java.io.IOException;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;

/**
 * A {@code char} value stored in blockchain.
 */
@Immutable
public final class CharValue extends StorageValue {
	static final byte SELECTOR = 3;

	/**
	 * The value.
	 */
	public final char value;

	/**
	 * Builds a {@code char} value.
	 * 
	 * @param value the value
	 */
	public CharValue(char value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Character.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CharValue && ((CharValue) other).value == value;
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
			return Character.compare(value, ((CharValue) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		context.writeChar(value);
	}
}