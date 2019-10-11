package takamaka.blockchain;

import java.math.BigInteger;

import takamaka.blockchain.values.FloatValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update of a field states that a float field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfFloat extends AbstractUpdateOfField {

	private static final long serialVersionUID = 3816173313618262315L;

	/**
	 * The new value of the field.
	 */
	private final float value;

	/**
	 * Builds an update of an {@code float} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfFloat(StorageReference object, FieldSignature field, float value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new FloatValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfFloat && super.equals(other) && ((UpdateOfFloat) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.hashCode(value);
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Float.compare(value, ((UpdateOfFloat) other).value);
	}

	@Override
	public UpdateOfFloat contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfFloat(objectContextualized, field, value);
		else
			return this;
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.STORAGE_COST_PER_SLOT);
	}
}