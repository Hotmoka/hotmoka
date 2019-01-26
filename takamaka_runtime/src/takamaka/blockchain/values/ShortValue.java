package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class ShortValue implements StorageValue {
	public final short value;

	public ShortValue(short value) {
		this.value = value;
	}
}