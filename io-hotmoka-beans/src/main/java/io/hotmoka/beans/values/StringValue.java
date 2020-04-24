package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A string stored in blockchain.
 */
@Immutable
public final class StringValue implements StorageValue {

	private static final long serialVersionUID = -2043931695947289129L;
	static final byte SELECTOR = 10;

	/**
	 * The string.
	 */
	public final String value;

	/**
	 * Builds a string that can be stored in blockchain.
	 * 
	 * @param value the string
	 */
	public StringValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StringValue && ((StringValue) other).value.equals(value);
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
			return value.compareTo(((StringValue) other).value);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		oos.writeUTF(value);
	}
}