package io.hotmoka.beans.values;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;

/**
 * A string stored in blockchain.
 */
@Immutable
public final class StringValue extends StorageValue {
	static final byte SELECTOR = 10;
	static final byte SELECTOR_EMPTY_STRING = 13;

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
		if (value == null)
			throw new IllegalArgumentException("value cannot be null");

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
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(value));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if ("".equals(value))
			context.writeByte(SELECTOR_EMPTY_STRING);
		else {
			context.writeByte(SELECTOR);
			context.writeUTF(value);
		}
	}
}