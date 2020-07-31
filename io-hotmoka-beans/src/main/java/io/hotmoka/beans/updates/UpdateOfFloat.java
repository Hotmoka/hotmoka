package io.hotmoka.beans.updates;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that a float field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfFloat extends AbstractUpdateOfField {
	final static byte SELECTOR = 10;

	/**
	 * The new value of the field.
	 */
	public final float value;

	/**
	 * Builds an update of an {@code float} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfFloat(StorageReference object, FieldSignature field, float value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new FloatValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfFloat && super.equals(other) && ((UpdateOfFloat) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.hashCode(value);
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Float.compare(value, ((UpdateOfFloat) other).value);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()));
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		oos.writeFloat(value);
	}
}