package io.takamaka.code.blockchain;

import java.math.BigInteger;

import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.values.ShortValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;

/**
 * An update of a field states that a short field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfShort extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2226960173435837206L;

	/**
	 * The new value of the field.
	 */
	private final short value;

	/**
	 * Builds an update of an {@code short} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfShort(StorageReference object, FieldSignature field, short value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new ShortValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfShort && super.equals(other) && ((UpdateOfShort) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value;
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Short.compare(value, ((UpdateOfShort) other).value);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()));
	}
}