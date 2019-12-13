package io.takamaka.code.blockchain.values;

import java.math.BigInteger;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.GasCostModel;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A {@code byte} value stored in blockchain.
 */
@Immutable
public final class ByteValue implements StorageValue {

	private static final long serialVersionUID = -387404205334801866L;

	/**
	 * The value.
	 */
	public final byte value;

	/**
	 * Builds a {@code byte} value.
	 * 
	 * @param value the value
	 */
	public ByteValue(byte value) {
		this.value = value;
	}

	@Override
	public Byte deserialize(AbstractBlockchain blockchain) {
		return value;
	}

	@Override
	public String toString() {
		return Byte.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ByteValue && ((ByteValue) other).value == value;
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
			return Byte.compare(value, ((ByteValue) other).value);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot()).add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()));
	}
}