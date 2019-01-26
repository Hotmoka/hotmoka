package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class StringValue implements StorageValue {
	public final String value;

	public StringValue(String value) {
		this.value = value;
	}
}