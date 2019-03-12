package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class CharValue implements StorageValue {
	public final char value;

	public CharValue(char value) {
		this.value = value;
	}

	@Override
	public Character deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}

	@Override
	public String toString() {
		return Character.toString(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CharValue && ((CharValue) other).value == value;
	}

	@Override
	public int hashCode() {
		return value;
	}
}