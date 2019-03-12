package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class BooleanValue implements StorageValue {
	public final boolean value;

	public BooleanValue(boolean value) {
		this.value = value;
	}

	@Override
	public Boolean deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BooleanValue && ((BooleanValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return Boolean.hashCode(value);
	}
}