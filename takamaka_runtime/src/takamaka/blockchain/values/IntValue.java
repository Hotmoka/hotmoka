package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.lang.Immutable;

/**
 * An {@code int} value stored in blockchain.
 */
@Immutable
public final class IntValue implements StorageValue {

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