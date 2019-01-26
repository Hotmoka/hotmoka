package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class LongValue implements StorageValue {
	public final long value;

	public LongValue(long value) {
		this.value = value;
	}
}