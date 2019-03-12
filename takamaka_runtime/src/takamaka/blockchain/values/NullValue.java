package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class NullValue implements StorageValue {
	public final static NullValue INSTANCE = new NullValue();

	private NullValue() {}

	@Override
	public Object deserialize(Blockchain blockchain) throws TransactionException {
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