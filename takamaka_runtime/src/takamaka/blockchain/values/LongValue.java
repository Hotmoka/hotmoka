package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class LongValue implements StorageValue {
	public final long value;

	public LongValue(long value) {
		this.value = value;
	}

	@Override
	public Long deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) throws TransactionException {
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
}