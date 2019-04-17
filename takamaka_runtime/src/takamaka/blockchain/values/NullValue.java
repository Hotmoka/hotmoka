package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.lang.Immutable;

/**
 * The {@code null} value stored in blockchain.
 */
@Immutable
public final class NullValue implements StorageValue {
	public final static NullValue INSTANCE = new NullValue();

	/**
	 * Builds the {@code null} value. This constructor is private, so that
	 * {@link takamaka.blockchain.values.NullValue#INSTANCE} is the singleton
	 * value existing of this class.
	 */
	private NullValue() {}

	@Override
	public Object deserialize(AbstractBlockchain blockchain) {
		return null;
	}

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NullValue;
	}

	@Override
	public int hashCode() {
		return 13011973;
	}

	@Override
	public int compareTo(StorageValue other) {
		return getClass().getName().compareTo(other.getClass().getName());
	}
}