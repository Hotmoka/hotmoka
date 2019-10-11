package takamaka.blockchain;

import java.math.BigInteger;

import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update of a field states that the field of storage type
 * of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfStorage extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2841032887225289222L;

	/**
	 * The new value of the field.
	 */
	private final StorageReference value;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfStorage(StorageReference object, FieldSignature field, StorageReference value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfStorage && super.equals(other) && ((UpdateOfStorage) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public boolean isEager() {
		return false;
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfStorage) other).value);
	}

	@Override
	public UpdateOfStorage contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);
		StorageReference valueContextualized = value.contextualizeAt(where);

		if (object != objectContextualized || value != valueContextualized)
			return new UpdateOfStorage(objectContextualized, field, valueContextualized);
		else
			return this;
	}

	@Override
	public BigInteger size() {
		return super.size().add(value.size());
	}
}