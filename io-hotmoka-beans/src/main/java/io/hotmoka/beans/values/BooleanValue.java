package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;

/**
 * A {@code boolean} value stored in blockchain.
 */
@Immutable
public final class BooleanValue implements StorageValue {

	private static final long serialVersionUID = -1793831545775110659L;
	static final byte SELECTOR_TRUE = 0;
	static final byte SELECTOR_FALSE = 1;

	/**
	 * The true Boolean value.
	 */
	public final static BooleanValue TRUE = new BooleanValue(true);

	/**
	 * The false Boolean value.
	 */
	public final static BooleanValue FALSE = new BooleanValue(false);

	/**
	 * The value.
	 */
	public final boolean value;

	/**
	 * Builds a {@code boolean} value.
	 * 
	 * @param value the value
	 */
	public BooleanValue(boolean value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BooleanValue && ((BooleanValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Boolean.compare(value, ((BooleanValue) other).value);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		if (value)
			oos.writeByte(SELECTOR_TRUE);
		else
			oos.writeByte(SELECTOR_FALSE);
	}
}