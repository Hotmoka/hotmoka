package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.lang.Immutable;

@Immutable
public final class DoubleValue implements StorageValue {
	public final double value;

	public DoubleValue(double value) {
		this.value = value;
	}

	@Override
	public Double deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) {
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
}