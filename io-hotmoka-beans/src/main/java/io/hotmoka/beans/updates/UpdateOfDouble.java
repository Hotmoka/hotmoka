package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that a double field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfDouble extends AbstractUpdateOfField {
	final static byte SELECTOR = 7;

	/**
	 * The new value of the field.
	 */
	public final double value;

	/**
	 * Builds an update of an {@code int} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfDouble(StorageReference object, FieldSignature field, double value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new DoubleValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfDouble && super.equals(other) && ((UpdateOfDouble) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Double.hashCode(value);
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Double.compare(value, ((UpdateOfDouble) other).value);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		super.into(context);
		context.oos.writeDouble(value);
	}
}