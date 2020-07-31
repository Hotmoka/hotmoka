package io.hotmoka.beans.updates;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;

/**
 * An update of a field states that the {@link java.lang.String}
 * field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfString extends AbstractUpdateOfField {
	final static byte SELECTOR = 17;

	/**
	 * The new value of the field.
	 */
	public final String value;

	/**
	 * Builds an update of a {@link java.lang.String} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfString(StorageReference object, FieldSignature field, String value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new StringValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfString && super.equals(other) && ((UpdateOfString) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfString) other).value);
	}

	@Override
	public boolean isEager() {
		// a lazy String could be stored into a lazy Object or Serializable or Comparable or CharSequence field
		return field.type.equals(ClassType.STRING);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(value));
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeByte(SELECTOR);
		super.into(oos);
		oos.writeUTF(value);
	}
}