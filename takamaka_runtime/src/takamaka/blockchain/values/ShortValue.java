package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class ShortValue implements StorageValue {
	public final short value;

	public ShortValue(short value) {
		this.value = value;
	}

	@Override
	public Short deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}
}