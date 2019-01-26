package takamaka.blockchain.values;

import java.math.BigInteger;

import takamaka.lang.Immutable;

@Immutable
public final class BigIntegerValue implements StorageValue {
	public final BigInteger value;

	public BigIntegerValue(BigInteger value) {
		this.value = value;
	}
}