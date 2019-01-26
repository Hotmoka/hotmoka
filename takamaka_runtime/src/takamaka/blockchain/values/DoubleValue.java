package takamaka.blockchain.values;

import takamaka.lang.Immutable;

@Immutable
public final class DoubleValue implements StorageValue {
	public final double value;

	public DoubleValue(double value) {
		this.value = value;
	}
}