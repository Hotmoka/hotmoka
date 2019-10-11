package takamaka.blockchain;

import java.math.BigInteger;

import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update of a field states that a long field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfLong extends AbstractUpdateOfField {

	private static final long serialVersionUID = -2226960173435837206L;

	/**
	 * The new value of the field.
	 */
	private final long value;

	/**
	 * Builds an update of an {@code long} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfLong(StorageReference object, FieldSignature field, long value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new LongValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfLong && super.equals(other) && ((UpdateOfLong) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Long.hashCode(value);
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Long.compare(value, ((UpdateOfLong) other).value);
	}

	@Override
	public UpdateOfLong contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfLong(objectContextualized, field, value);
		else
			return this;
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.STORAGE_COST_PER_SLOT);
	}
}