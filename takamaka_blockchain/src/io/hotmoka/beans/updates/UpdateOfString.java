package io.hotmoka.beans.updates;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.takamaka.code.blockchain.ClassTypes;

/**
 * An update of a field states that the {@link java.lang.String}
 * field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfString extends AbstractUpdateOfField {

	private static final long serialVersionUID = 3772782491017435498L;

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
		return field.type.equals(ClassTypes.STRING);
	}
}