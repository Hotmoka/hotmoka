package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class StringValue implements StorageValue {
	public final String value;

	public StringValue(String value) {
		this.value = value;
	}

	@Override
	public String deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) throws TransactionException {
		// we clone the value, so that the alias behavior of values coming from outside the blockchain is fixed
		return new String(value);
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StringValue && ((StringValue) other).value.equals(value);
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
			return value.compareTo(((StringValue) other).value);
	}
}