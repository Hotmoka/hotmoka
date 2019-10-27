package takamaka.blockchain.values;

import java.math.BigInteger;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.GasCosts;

/**
 * A big integer stored in blockchain.
 */
@Immutable
public final class BigIntegerValue implements StorageValue {

	private static final long serialVersionUID = 5290050934759989938L;

	/**
	 * The big integer.
	 */
	public final BigInteger value;

	/**
	 * Builds a big integer that can be stored in blockchain.
	 * 
	 * @param value the big integer
	 */
	public BigIntegerValue(BigInteger value) {
		this.value = value;
	}

	@Override
	public BigInteger deserialize(AbstractBlockchain blockchain) {
		// we clone the value, so that the alias behavior of values coming from outside the blockchain is fixed
		return new BigInteger(value.toString());
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BigIntegerValue && ((BigIntegerValue) other).value.equals(value);
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
			return value.compareTo(((BigIntegerValue) other).value);
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.storageCostOf(value));
	}
}