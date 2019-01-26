package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class FloatValue implements StorageValue {
	public final float value;

	public FloatValue(float value) {
		this.value = value;
	}
}