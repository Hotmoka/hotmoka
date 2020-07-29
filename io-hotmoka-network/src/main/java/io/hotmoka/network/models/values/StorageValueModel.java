package io.hotmoka.network.models.values;

import java.math.BigInteger;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;

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
	 * Builds the model of a storage value.
	 * 
	 * @param parent the storage value
	 */
	public StorageValueModel(StorageValue parent) {
    	if (parent == null)
    		throw new InternalFailureException("unexpected null storage value");
    	else if (parent instanceof StorageReference) {
    		value = null;
    		reference = new StorageReferenceModel((StorageReference) parent);
    		enumClassName = null;
    		name = null;
    	}
    	else if (parent == NullValue.INSTANCE) {
    		value = null;
    		reference = null;
    		enumClassName = null;
    		name = null;
    	}
    	else if (parent instanceof EnumValue) {
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
     * Yields the storage value corresponding to this value, assuming that
     * its type is the given one.
     * 
     * @param type the assumed type
     * @return the storage value
     */
    public StorageValue toBean(StorageType type) {
    	if (type == null)
    		throw new InternalFailureException("unexpected null storage value type");
    	else if (type instanceof ClassType) {
    		if (value == null)
    			return NullValue.INSTANCE;
    		else if (enumClassName != null)
    			if (name == null)
    				throw new InternalFailureException("unexpected null name for the element of an enum");
    			else
    				return new EnumValue(enumClassName, name);
    		else if (type.equals(ClassType.BIG_INTEGER))
    			return new BigIntegerValue(new BigInteger(value));
    		else if (type.equals(ClassType.STRING))
    			return new StringValue(value);
    		else if (reference == null)
            	throw new InternalFailureException("unexpected null reference");
            else
            	return reference.toBean();
    	}
    	else if (value == null)
    		throw new InternalFailureException("unexpected null value");
    	else if (type == BasicTypes.BOOLEAN)
            return new BooleanValue(Boolean.parseBoolean(value));
    	else if (type == BasicTypes.BYTE)
            return new ByteValue(Byte.parseByte(value));
    	else if (type == BasicTypes.CHAR)
            return new CharValue(value.charAt(0));
    	else if (type == BasicTypes.SHORT)
            return new ShortValue(Short.parseShort(value));
    	else if (type == BasicTypes.INT)
            return new IntValue(Integer.parseInt(value));
    	else if (type == BasicTypes.LONG)
            return new LongValue(Long.parseLong(value));
    	else if (type == BasicTypes.FLOAT)
            return new FloatValue(Float.parseFloat(value));
    	else if (type == BasicTypes.DOUBLE)
            return new DoubleValue(Double.parseDouble(value));
    	else
        	throw new InternalFailureException("unepected value type " + type);
    }
}