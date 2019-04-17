package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.lang.Immutable;

/**
 * A {@code boolean} value stored in blockchain.
 */
@Immutable
public final class BooleanValue implements StorageValue {

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
	public Boolean deserialize(AbstractBlockchain blockchain) {
		return value;
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
}