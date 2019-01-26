package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class IntValue implements StorageValue {
	public final int value;

	public IntValue(int value) {
		this.value = value;
	}
}