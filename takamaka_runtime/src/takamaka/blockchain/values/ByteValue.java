package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class ByteValue implements StorageValue {
	public final byte value;

	public ByteValue(byte value) {
		this.value = value;
	}

	@Override
	public Byte deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) throws TransactionException {
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
}