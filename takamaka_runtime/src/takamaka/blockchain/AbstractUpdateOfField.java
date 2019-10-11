package takamaka.blockchain;

import java.math.BigInteger;

import takamaka.blockchain.values.StorageReference;
import takamaka.lang.Immutable;

/**
 * An update of a field states that the field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public abstract class AbstractUpdateOfField extends UpdateOfField {

	private static final long serialVersionUID = -3457326373592574148L;

	/**
	 * The field that is modified.
	 */
	protected final FieldSignature field;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 */
	protected AbstractUpdateOfField(StorageReference object, FieldSignature field) {
		super(object);

		this.field = field;
	}

	@Override
	public FieldSignature getField() {
		return field;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AbstractUpdateOfField && super.equals(other) && ((AbstractUpdateOfField) other).field.equals(field);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ field.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return field.compareTo(((AbstractUpdateOfField) other).field);
	}

	@Override
	public final boolean isForSamePropertyAs(Update other) {
		return super.isForSamePropertyAs(other) && field.equals(((AbstractUpdateOfField) other).field);
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(field.size());
	}
}