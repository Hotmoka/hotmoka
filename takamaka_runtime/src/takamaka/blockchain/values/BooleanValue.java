package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class BooleanValue implements StorageValue {
	public final boolean value;

	public BooleanValue(boolean value) {
		this.value = value;
	}
}