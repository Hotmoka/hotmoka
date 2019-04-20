package takamaka.blockchain.values;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.lang.Immutable;

/**
 * A {@code short} value stored in blockchain.
 */
@Immutable
public final class ShortValue implements StorageValue {

	private static final long serialVersionUID = 7093462121703197078L;

	/**
	 * The value.
	 */
	public final short value;

	/**
	 * Builds a {@code short} value.
	 * 
	 * @param value the value
	 */
	public ShortValue(short value) {
		this.value = value;
	}

	@Override
	public Short deserialize(AbstractBlockchain blockchain) {
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