package takamaka.blockchain;

import java.math.BigInteger;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update of a field states that the {@link java.math.BigInteger}
 * field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfBigInteger extends AbstractUpdateOfField {

	private static final long serialVersionUID = 7267869415886381162L;

	/**
	 * The new value of the field.
	 */
	private final BigInteger value;

	/**
	 * Builds an update of a {@link java.math.BigInteger} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfBigInteger(StorageReference object, FieldSignature field, BigInteger value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new BigIntegerValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfBigInteger && super.equals(other) && ((UpdateOfBigInteger) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfBigInteger) other).value);
	}

	@Override
	public UpdateOfBigInteger contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfBigInteger(objectContextualized, field, value);
		else
			return this;
	}

	@Override
	public boolean isEager() {
		// a BigInteger could be stored into a lazy Object or Serializable or Comparable or Number field
		return !field.type.equals(ClassType.OBJECT)
			&& !((ClassType) field.type).name.equals("java.io.Serializable")
			&& !((ClassType) field.type).name.equals("java.lang.Comparable")
			&& !((ClassType) field.type).name.equals("java.lang.Number");
	}
}