package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class LongValue implements StorageValue {
	public final long value;

	public LongValue(long value) {
		this.value = value;
	}

	@Override
	public Long deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}
}