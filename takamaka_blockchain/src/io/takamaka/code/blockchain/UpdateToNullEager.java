package io.takamaka.code.blockchain;

import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.values.NullValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;

/**
 * An update that states that the field of a given storage object has been
 * modified to {@code null}. The field has eager type.
 * Updates are stored in blockchain and describe the shape of storage objects.
 */
@Immutable
public final class UpdateToNullEager extends AbstractUpdateOfField {

	private static final long serialVersionUID = 6580694465259569417L;

	/**
	 * Builds an update of a {@link java.math.BigInteger} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 */
	public UpdateToNullEager(StorageReference object, FieldSignature field) {
		super(object, field);
	}

	@Override
	public StorageValue getValue() {
		return NullValue.INSTANCE;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateToNullEager && super.equals(other);
	}

	@Override
	public boolean isEager() {
		return true;
	}
}