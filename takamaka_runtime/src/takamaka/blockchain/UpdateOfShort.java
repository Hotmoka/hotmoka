package takamaka.blockchain;

import takamaka.blockchain.values.ShortValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update of a field states that a short field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfShort extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2226960173435837206L;

	/**
	 * The new value of the field.
	 */
	private final short value;

	/**
	 * Builds an update of an {@code short} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfShort(StorageReference object, FieldSignature field, short value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new ShortValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfShort && super.equals(other) && ((UpdateOfShort) other).value == value;
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
			return Short.compare(value, ((UpdateOfShort) other).value);
	}

	@Override
	public UpdateOfShort contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfShort(objectContextualized, field, value);
		else
			return this;
	}
}