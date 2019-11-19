package io.takamaka.code.blockchain;

import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.values.NullValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;

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