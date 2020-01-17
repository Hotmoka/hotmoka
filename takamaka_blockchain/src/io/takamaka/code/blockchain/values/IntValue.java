package io.takamaka.code.blockchain.values;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * An {@code int} value stored in blockchain.
 */
@Immutable
public final class IntValue implements StorageValue {

	private static final long serialVersionUID = -8632377447068901935L;

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
	public Integer deserialize(AbstractBlockchain blockchain) {
		return value;
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
}