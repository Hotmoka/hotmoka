package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;

/**
 * The {@code null} value stored in blockchain.
 */
@Immutable
public final class NullValue extends StorageValue {
	static final byte SELECTOR = 8;

	public final static NullValue INSTANCE = new NullValue();

	/**
	 * Builds the {@code null} value. This constructor is private, so that
	 * {@link io.hotmoka.beans.values.NullValue#INSTANCE} is the singleton
	 * value existing of this class.
	 */
	private NullValue() {}

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NullValue;
	}

	@Override
	public int hashCode() {
		return 13011973;
	}

	@Override
	public int compareTo(StorageValue other) {
		return getClass().getName().compareTo(other.getClass().getName());
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.write(SELECTOR);
	}
}