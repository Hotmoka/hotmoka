package takamaka.blockchain;

import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update of a field states that an integer field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfInt extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2226960173435837206L;

	/**
	 * The new value of the field.
	 */
	private final int value;

	/**
	 * Builds an update of an {@code int} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfInt(StorageReference object, FieldSignature field, int value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new IntValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfInt && super.equals(other) && ((UpdateOfInt) other).value == value;
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
			return Integer.compare(value, ((UpdateOfInt) other).value);
	}

	@Override
	public UpdateOfInt contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfInt(objectContextualized, field, value);
		else
			return this;
	}
}