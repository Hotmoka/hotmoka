package io.hotmoka.beans.updates;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update that states that the field of a given storage object has been
 * modified to {@code null}. The field is declared with a lazy type.
 * Updates are stored in blockchain and describe the shape of storage objects.
 */
@Immutable
public final class UpdateToNullLazy extends AbstractUpdateOfField {

	private static final long serialVersionUID = -8022647081096859934L;

	/**
	 * Builds an update of a {@link java.math.BigInteger} field of lazy type.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 */
	public UpdateToNullLazy(StorageReference object, FieldSignature field) {
		super(object, field);
	}

	@Override
	public StorageValue getValue() {
		return NullValue.INSTANCE;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateToNullLazy && super.equals(other);
	}

	@Override
	public boolean isEager() {
		return false;
	}
}