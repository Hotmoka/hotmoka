package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class ByteValue implements StorageValue {
	public final byte value;

	public ByteValue(byte value) {
		this.value = value;
	}

	@Override
	public Byte deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}
}