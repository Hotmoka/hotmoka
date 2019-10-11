package takamaka.blockchain;

import java.math.BigInteger;

import takamaka.blockchain.values.EnumValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.lang.Immutable;

/**
 * An update that states that the enumeration
 * field of a given storage object has been
 * modified to a given value. The type of the field is lazy.
 * Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfEnumLazy extends AbstractUpdateOfField {

	private static final long serialVersionUID = 1502304606798344063L;

	/**
	 * The name of the enumeration class whose element is being assigned to the field.
	 */
	private final String enumClassName;

	/**
	 * The name of the enumeration value put as new value of the field.
	 */
	private final String name;

	/**
	 * Builds an update of an enumeration field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param enumClassName the name of the enumeration class whose element is being assigned to the field
	 * @param name the name of the enumeration value put as new value of the field
	 */
	public UpdateOfEnumLazy(StorageReference object, FieldSignature field, String enumClassName, String name) {
		super(object, field);

		this.enumClassName = enumClassName;
		this.name = name;
	}

	@Override
	public StorageValue getValue() {
		return new EnumValue(enumClassName, name);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfEnumLazy && super.equals(other) && ((UpdateOfEnumLazy) other).name.equals(name)
			&& ((UpdateOfEnumLazy) other).enumClassName.equals(enumClassName);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ name.hashCode() ^ enumClassName.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;

		diff = enumClassName.compareTo(((UpdateOfEnumLazy) other).enumClassName);
		if (diff != 0)
			return diff;
		else
			return name.compareTo(((UpdateOfEnumLazy) other).name);
	}

	@Override
	public UpdateOfEnumLazy contextualizeAt(TransactionReference where) {
		StorageReference objectContextualized = object.contextualizeAt(where);

		if (object != objectContextualized)
			return new UpdateOfEnumLazy(objectContextualized, field, enumClassName, name);
		else
			return this;
	}

	@Override
	public boolean isEager() {
		return false;
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.storageCostOf(enumClassName)).add(GasCosts.storageCostOf(name));
	}
}