package io.hotmoka.beans.updates;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that the field of storage type
 * of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfStorage extends UpdateOfField {
	final static byte SELECTOR = 16;

	/**
	 * The new value of the field.
	 */
	public final StorageReference value;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfStorage(StorageReference object, FieldSignature field, StorageReference value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfStorage && super.equals(other) && ((UpdateOfStorage) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public boolean isEager() {
		return false;
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfStorage) other).value);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(value.size(gasCostModel));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		super.into(context);
		value.intoWithoutSelector(context);
	}
}