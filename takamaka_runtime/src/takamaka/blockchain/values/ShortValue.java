package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.blockchain.BlockchainClassLoader;
import takamaka.lang.Immutable;

@Immutable
public final class ShortValue implements StorageValue {
	public final short value;

	public ShortValue(short value) {
		this.value = value;
	}

	@Override
	public Short deserialize(BlockchainClassLoader classLoader, AbstractBlockchain blockchain) {
		return value;
	}

	@Override
	public String toString() {
		return Short.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ShortValue && ((ShortValue) other).value == value;
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
			return Short.compare(value, ((ShortValue) other).value);
	}
}