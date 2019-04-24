package takamaka.blockchain;

import java.io.Serializable;

import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update is a triple statement that the field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class Update implements Serializable, Comparable<Update> {

	private static final long serialVersionUID = 1921751386937488337L;

	/**
	 * The storage reference of the object whose field is modified.
	 */
	public final StorageReference object;

	/**
	 * The field that is modified.
	 */
	public final FieldSignature field;

	/**
	 * The new value of the field.
	 */
	public final StorageValue value;

	/**
	 * This special name is used for the pseudo-field that keeps track
	 * the class of a storage object.
	 */
	private final static String CLASS_TAG_FIELD_NAME = "@class";

	/**
	 * Used as dummy type for the pseudo-field {@link takamaka.blockchain.Update#CLASS_TAG_FIELD_NAME}.
	 */
	private final static BooleanValue VALUE_FOR_CLASS_TAG = new BooleanValue(true);

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public Update(StorageReference object, FieldSignature field, StorageValue value) {
		this.object = object;
		this.field = field;
		this.value = value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Update && ((Update) other).object.equals(object)
				&& ((Update) other).field.equals(field) && ((Update) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return object.hashCode() ^ field.hashCode() ^ value.hashCode();
	}

	/**
	 * Builds an update for the special pseudo-field that tracks the class of
	 * a storage object.
	 * 
	 * @param object the storage reference of the object whose class is tracked
	 * @param className the class of the object
	 * @return the update
	 */
	public static Update mkForClassTag(StorageReference object, String className) {
		return new Update(object, new FieldSignature(className, CLASS_TAG_FIELD_NAME, BasicTypes.BOOLEAN), VALUE_FOR_CLASS_TAG);
	}

	@Override
	public String toString() {
		return "<" + object + "|" + field + "|" + value + ">";
	}

	/**
	 * Determines if this is an update for the special pseudo-field used
	 * to track the class of a storage object.
	 * 
	 * @return true if and only if this is an update of that special field
	 */
	public boolean isClassTag() {
		return CLASS_TAG_FIELD_NAME.equals(field.name);
	}

	@Override
	public int compareTo(Update other) {
		int diff = object.compareTo(other.object);
		if (diff != 0)
			return diff;

		diff = field.compareTo(other.field);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(other.value);
	}

	/**
	 * Yields an update derived from this, by assuming that the current transaction
	 * is the given one.
	 * 
	 * @param where the transaction
	 * @return the resulting update
	 */
	public Update contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);
		StorageValue valueContextualized = (value instanceof StorageReference) ? ((StorageReference) value).contextualizeAt(where) : value;

		if (object != objectContextualized || value != valueContextualized)
			return new Update(objectContextualized, field, valueContextualized);
		else
			return this;
	}
}