package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.lang.Immutable;

@Immutable
public final class IntValue implements StorageValue {
	public final int value;

	public IntValue(int value) {
		this.value = value;
	}

	@Override
	public Integer deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) {
		return value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof IntValue && ((IntValue) other).value == value;
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
			return Integer.compare(value, ((IntValue) other).value);
	}
}