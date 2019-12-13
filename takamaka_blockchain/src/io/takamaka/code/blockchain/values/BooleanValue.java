package io.takamaka.code.blockchain.values;

import java.math.BigInteger;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.GasCostModel;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A {@code boolean} value stored in blockchain.
 */
@Immutable
public final class BooleanValue implements StorageValue {

	private static final long serialVersionUID = -1793831545775110659L;

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

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot())
			.add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()));
	}
}