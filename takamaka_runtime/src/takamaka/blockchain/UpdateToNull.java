package takamaka.blockchain;

import takamaka.blockchain.values.NullValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update of a field states that the field of a given storage object has been
 * modified to {@code null}. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateToNull extends AbstractUpdateOfField {

	private static final long serialVersionUID = 6580694465259569417L;

	/**
	 * Builds an update of a {@link java.math.BigInteger} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 */
	public UpdateToNull(StorageReference object, FieldSignature field) {
		super(object, field);
	}

	@Override
	public StorageValue getValue() {
		return NullValue.INSTANCE;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateToNull && super.equals(other);
	}

	@Override
	public UpdateToNull contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateToNull(objectContextualized, field);
		else
			return this;
	}
}