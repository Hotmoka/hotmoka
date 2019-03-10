package takamaka.blockchain.values;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class StringValue implements StorageValue {
	public final String value;

	public StringValue(String value) {
		this.value = value;
	}

	@Override
	public String deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}
}