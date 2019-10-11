package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.GasCosts;
import takamaka.lang.Immutable;

/**
 * A {@code long} value stored in blockchain.
 */
@Immutable
public final class LongValue implements StorageValue {

	private static final long serialVersionUID = -8313637885821044349L;

	/**
	 * The value.
	 */
	public final long value;

	/**
	 * Builds a {@code long} value.
	 * 
	 * @param value the value
	 */
	public LongValue(long value) {
		this.value = value;
	}

	@Override
	public Long deserialize(AbstractBlockchain blockchain) {
		return value;
	}

	@Override
	public String toString() {
		return Long.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LongValue && ((LongValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Long.compare(value, ((LongValue) other).value);
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.STORAGE_COST_PER_SLOT);
	}
}