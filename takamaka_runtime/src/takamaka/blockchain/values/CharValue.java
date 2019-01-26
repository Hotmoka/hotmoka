package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class CharValue implements StorageValue {
	public final char value;

	public CharValue(char value) {
		this.value = value;
	}
}