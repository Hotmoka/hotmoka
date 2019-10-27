package takamaka.blockchain;

import java.math.BigInteger;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.values.DoubleValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;

/**
 * An update of a field states that a double field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfDouble extends AbstractUpdateOfField {

	private static final long serialVersionUID = 3816173313618262315L;

	/**
	 * The new value of the field.
	 */
	private final double value;

	/**
	 * Builds an update of an {@code int} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfDouble(StorageReference object, FieldSignature field, double value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new DoubleValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfDouble && super.equals(other) && ((UpdateOfDouble) other).value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Double.hashCode(value);
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Double.compare(value, ((UpdateOfDouble) other).value);
	}

	@Override
	public UpdateOfDouble contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfDouble(objectContextualized, field, value);
		else
			return this;
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.STORAGE_COST_PER_SLOT);
	}
}