package io.hotmoka.network.models.values;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.values.*;

import java.math.BigInteger;

/**
 * The model of a storage value.
 */
@Immutable
public class StorageValueModel {

	/**
	 * Used for primitive values, big integers, strings and null.
	 * For the null value, this field holds exactly null, not the string "null".
	 */
	public final String value;

	/**
	 * Used for storage references.
	 */
	public final StorageReferenceModel reference;

	/**
	 * Used for enumeration values only: it is the name of the class.
	 */
	public final String enumClassName;

	/**
	 * Used for enumeration values only: it is the name of the element in the enumeration.
	 */
	public final String name;

	/**
	 * The value type of this storage value
	 */
	public final String valueType;


	/**
	 * Builds the model of a storage value.
	 * 
	 * @param parent the storage value
	 */
	public StorageValueModel(StorageValue parent) {
    	if (parent == null)
    		throw new InternalFailureException("unexpected null storage value");

		this.valueType = getTypeValue(parent);
		if (this.valueType.equals(StorageReference.class.getSimpleName())) {
    		value = null;
    		reference = new StorageReferenceModel((StorageReference) parent);
    		enumClassName = null;
    		name = null;
    	}
    	else if (this.valueType.equals(NullValue.class.getSimpleName())) {
    		value = null;
    		reference = null;
    		enumClassName = null;
    		name = null;
    	}
    	else if (this.valueType.equals(EnumValue.class.getSimpleName())) {
    		EnumValue parentAsEnumValue = (EnumValue) parent;
    		value = null;
    		reference = null;
    		enumClassName = parentAsEnumValue.enumClassName;
    		name = parentAsEnumValue.name;
    	}
    	else {
    		value = parent.toString();
    		reference = null;
    		enumClassName = null;
    		name = null;
    	}
    }

	/**
	 * Yields the name of the instance of the parent storage value
	 * @param parent the parent
	 * @return the name of the instance
	 */
	private String getTypeValue(StorageValue parent) {

		if (parent instanceof BigIntegerValue)
			return BigIntegerValue.class.getSimpleName();
		else if (parent instanceof BooleanValue)
			return BooleanValue.class.getSimpleName();
		else if (parent instanceof ByteValue)
			return ByteValue.class.getSimpleName();
		else if (parent instanceof CharValue)
			return CharValue.class.getSimpleName();
		else if (parent instanceof DoubleValue)
			return DoubleValue.class.getSimpleName();
		else if (parent instanceof EnumValue)
			return EnumValue.class.getSimpleName();
		else if (parent instanceof FloatValue)
			return FloatValue.class.getSimpleName();
		else if (parent instanceof IntValue)
			return IntValue.class.getSimpleName();
		else if (parent instanceof LongValue)
			return LongValue.class.getSimpleName();
		else if (parent instanceof NullValue)
			return NullValue.class.getSimpleName();
		else if (parent instanceof ShortValue)
			return ShortValue.class.getSimpleName();
		else if (parent instanceof StorageReference)
			return StorageReference.class.getSimpleName();
		else if (parent instanceof StringValue)
			return StringValue.class.getSimpleName();
		else
			throw new InternalFailureException("unexpected storage value");
	}


	/**
	 * Yields the storage value corresponding to this storage value
	 * @return the storage value
	 */
	public StorageValue toBean() {

		if (this.valueType.equals(NullValue.class.getSimpleName()))
			return NullValue.INSTANCE;
		else if (this.valueType.equals(StorageReference.class.getSimpleName())) {
			if (this.reference == null)
				throw new InternalFailureException("unexpected null reference");
			else
				return reference.toBean();
		}
		else if (this.valueType.equals(EnumValue.class.getSimpleName())) {
			if (name == null)
				throw new InternalFailureException("unexpected null name for the element of an enum");
			else if (enumClassName == null)
				throw new InternalFailureException("unexpected null enumClassName for the enum class");
			else
				return new EnumValue(enumClassName, name);
		} else if (this.value == null)
			throw new InternalFailureException("unexpected null value");
		else if (this.valueType.equals(BigIntegerValue.class.getSimpleName()))
			return new BigIntegerValue(new BigInteger(value));
		else if (this.valueType.equals(StringValue.class.getSimpleName()))
			return new StringValue((value));
		else if (this.valueType.equals(BooleanValue.class.getSimpleName()))
			return new BooleanValue(Boolean.parseBoolean(value));
		else if (this.valueType.equals(ByteValue.class.getSimpleName()))
			return new ByteValue(Byte.parseByte(value));
		else if (this.valueType.equals(CharValue.class.getSimpleName()))
			return new CharValue(value.charAt(0));
		else if (this.valueType.equals(ShortValue.class.getSimpleName()))
			return new ShortValue(Short.parseShort(value));
		else if (this.valueType.equals(IntValue.class.getSimpleName()))
			return new IntValue(Integer.parseInt(value));
		else if (this.valueType.equals(LongValue.class.getSimpleName()))
			return new LongValue(Long.parseLong(value));
		else if (this.valueType.equals(FloatValue.class.getSimpleName()))
			return new FloatValue(Float.parseFloat(value));
		else if (this.valueType.equals(DoubleValue.class.getSimpleName()))
			return new DoubleValue(Double.parseDouble(value));
		else
			throw new InternalFailureException("unexpected value type " + valueType);
	}
}