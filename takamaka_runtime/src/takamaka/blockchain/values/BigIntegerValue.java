package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.TransactionException;
import takamaka.lang.Immutable;

@Immutable
public final class BigIntegerValue implements StorageValue {
	public final BigInteger value;

	public BigIntegerValue(BigInteger value) {
		this.value = value;
	}

	@Override
	public BigInteger deserialize(Blockchain blockchain) throws TransactionException {
		return value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BigIntegerValue && ((BigIntegerValue) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}