package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class DoubleValue implements StorageValue {
	public final double value;

	public DoubleValue(double value) {
		this.value = value;
	}

	@Override
	public Double deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}
}