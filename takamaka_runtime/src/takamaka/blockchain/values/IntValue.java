package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class IntValue implements StorageValue {
	public final int value;

	public IntValue(int value) {
		this.value = value;
	}

	@Override
	public Integer deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}
}