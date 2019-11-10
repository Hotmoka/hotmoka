package io.takamaka.code.blockchain.values;

import java.math.BigInteger;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.GasCosts;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A {@code double} value stored in blockchain.
 */
@Immutable
public final class DoubleValue implements StorageValue {

	private static final long serialVersionUID = -7582639158666797757L;

	/**
	 * The value.
	 */
	public final double value;

	/**
	 * Builds a {@code double} value.
	 * 
	 * @param value the value
	 */
	public DoubleValue(double value) {
		this.value = value;
	}

	@Override
	public Double deserialize(AbstractBlockchain blockchain) {
		return value;
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof DoubleValue && ((DoubleValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(value);
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return Double.compare(value, ((DoubleValue) other).value);
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.STORAGE_COST_PER_SLOT);
	}
}