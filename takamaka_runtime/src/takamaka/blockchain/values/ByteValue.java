package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class ByteValue implements StorageValue {
	public final byte value;

	public ByteValue(byte value) {
		this.value = value;
	}
}