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
}