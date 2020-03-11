package io.hotmoka.beans.updates;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that the field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public abstract class UpdateOfField extends Update {

	private static final long serialVersionUID = 555146480608531290L;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 */
	protected UpdateOfField(StorageReference object) {
		super(object);
	}

	/**
	 * Yields the value set into the updated field.
	 * 
	 * @return the value
	 */
	public abstract StorageValue getValue();

	/**
	 * Yields the field whose value is updated.
	 *
	 * @return the field
	 */
	public abstract FieldSignature getField();

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfField && super.equals(other);
	}

	@Override
	public final String toString() {
		return "<" + object + "|" + getField() + "|" + getValue() + ">";
	}
}