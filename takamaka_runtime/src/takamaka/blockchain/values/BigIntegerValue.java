package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.lang.Immutable;

@Immutable
public final class BigIntegerValue implements StorageValue {
	public final BigInteger value;

	public BigIntegerValue(BigInteger value) {
		this.value = value;
	}

	@Override
	public BigInteger deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) {
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
}