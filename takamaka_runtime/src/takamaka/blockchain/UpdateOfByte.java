package takamaka.blockchain;

import takamaka.blockchain.values.ByteValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update of a field states that a byte field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfByte extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2226960173435837206L;

	/**
	 * The new value of the field.
	 */
	private final byte value;

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
	public UpdateOfByte contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfByte(objectContextualized, field, value);
		else
			return this;
	}
}