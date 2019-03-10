package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class FloatValue implements StorageValue {
	public final float value;

	public FloatValue(float value) {
		this.value = value;
	}

	@Override
	public Float deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}
}