package io.hotmoka.beans.updates;

import java.io.IOException;
import java.io.ObjectOutputStream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that a byte field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfByte extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2226960173435837206L;
	final static byte SELECTOR = 5;

	/**
	 * The new value of the field.
	 */
	public final byte value;

	/**
	 * Builds an update of a {@code byte} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfByte(StorageReference object, FieldSignature field, byte value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new ByteValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfByte && super.equals(other) && ((UpdateOfByte) other).value == value;
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
			return Byte.compare(value, ((UpdateOfByte) other).value);
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		oos.writeByte(value);
	}
}