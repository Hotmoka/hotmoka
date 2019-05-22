package takamaka.blockchain;

import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.EnumValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update that states that the enumeration
 * field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfEnum extends AbstractUpdateOfField {

	private static final long serialVersionUID = 1502304606798344063L;

	/**
	 * The name of the enumeration value put as new value of the field.
	 */
	private final String name;

	/**
	 * Builds an update of an enumeration field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param name the name of the enumeration value put as new value of the field
	 */
	public UpdateOfEnum(StorageReference object, FieldSignature field, String name) {
		super(object, field);

		this.name = name;
	}

	@Override
	public StorageValue getValue() {
		return new EnumValue((ClassType) field.type, name);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfEnum && super.equals(other) && ((UpdateOfEnum) other).name == name;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ name.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return name.compareTo(name);
	}

	@Override
	public UpdateOfEnum contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfEnum(objectContextualized, field, name);
		else
			return this;
	}

	@Override
	public boolean isEager() {
		// an enumeration element could be stored into a lazy Object or Serializable or Comparable field
		return !field.type.equals(ClassType.OBJECT)
			&& !((ClassType) field.type).name.equals("java.io.Serializable")
			&& !((ClassType) field.type).name.equals("java.lang.Comparable");
	}
}