package io.hotmoka.network.models.values;

import java.math.BigInteger;

import io.hotmoka.beans.InternalFailureException;
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
import io.hotmoka.network.models.requests.MethodCallTransactionRequestModel;

/**
 * The model of a storage value.
 */
public class StorageValueModel {

	private static final String BIGINTEGER_NAME = BigInteger.class.getName();

	private static final String STRING_NAME = String.class.getName();

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public StorageReferenceModel getReference() {
		return reference;
	}

	public void setReference(StorageReferenceModel reference) {
		this.reference = reference;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getEnumElementName() {
		return enumElementName;
	}

	public void setEnumElementName(String enumElementName) {
		this.enumElementName = enumElementName;
	}

	/**
	 * Used for primitive values, big integers, strings and null.
	 * For the null value, this field holds exactly null, not the string "null".
	 */
	public String value;

	/**
	 * Used for storage references.
	 */
	public StorageReferenceModel reference;

	/**
	 * The type of the value. For storage references and {@code null}, this is {@code "reference"}.
	 */
	public String type;

	/**
	 * Used for enumeration values only: it is the name of the element in the enumeration.
	 */
	public String enumElementName;

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
    		type = "reference";
    		enumElementName = null;
    	}
    	else if (parent == NullValue.INSTANCE) {
    		value = null;
    		reference = null;
    		type = "reference";
    		enumElementName = null;
    	}
    	else if (parent instanceof EnumValue) {
    		EnumValue parentAsEnumValue = (EnumValue) parent;
    		value = null;
    		reference = null;
    		type = parentAsEnumValue.enumClassName;
    		enumElementName = parentAsEnumValue.name;
    	}
    	else if (parent instanceof BigIntegerValue) {
    		value = parent.toString();
    		reference = null;
    		type = BIGINTEGER_NAME;
    		enumElementName = null;
    	}
    	else if (parent instanceof StringValue) {
    		value = parent.toString();
    		reference = null;
    		type = STRING_NAME;
    		enumElementName = null;
    	}
    	else if (parent instanceof IntValue) {
    		value = parent.toString();
    		reference = null;
    		type = "int";
    		enumElementName = null;
    	}
    	else if (parent instanceof LongValue) {
    		value = parent.toString();
    		reference = null;
    		type = "long";
    		enumElementName = null;
    	}
    	else if (parent instanceof ShortValue) {
    		value = parent.toString();
    		reference = null;
    		type = "short";
    		enumElementName = null;
    	}
    	else if (parent instanceof CharValue) {
    		value = parent.toString();
    		reference = null;
    		type = "char";
    		enumElementName = null;
    	}
    	else if (parent instanceof FloatValue) {
    		value = parent.toString();
    		reference = null;
    		type = "float";
    		enumElementName = null;
    	}
    	else if (parent instanceof DoubleValue) {
    		value = parent.toString();
    		reference = null;
    		type = "double";
    		enumElementName = null;
    	}
    	else if (parent instanceof ByteValue) {
    		value = parent.toString();
    		reference = null;
    		type = "byte";
    		enumElementName = null;
    	}
    	else if (parent instanceof BooleanValue) {
    		value = parent.toString();
    		reference = null;
    		type = "boolean";
    		enumElementName = null;
    	}
    	else
    		throw new InternalFailureException("unexpected storage value of class " + parent.getClass().getName());
    }

	public StorageValueModel() {}

	/**
     * Yields the storage value corresponding to this value.
     * 
     * @return the storage value
     */
    public StorageValue toBean() {
    	if (enumElementName != null)
			return new EnumValue(type, enumElementName);
    	else if (type.equals("reference"))
    		if (reference == null)
    			return NullValue.INSTANCE;
            else
            	return reference.toBean();
    	else if (value == null)
    		throw new InternalFailureException("unexpected null value");
    	else if (type.equals(BIGINTEGER_NAME))
			return new BigIntegerValue(new BigInteger(value));
		else if (type.equals(STRING_NAME))
			return new StringValue(value);
		else if (type.equals("boolean"))
            return new BooleanValue(Boolean.parseBoolean(value));
    	else if (type.equals("byte"))
            return new ByteValue(Byte.parseByte(value));
    	else if (type.equals("char"))
            return new CharValue(value.charAt(0));
    	else if (type.equals("short"))
            return new ShortValue(Short.parseShort(value));
    	else if (type.equals("int"))
            return new IntValue(Integer.parseInt(value));
    	else if (type.equals("long"))
            return new LongValue(Long.parseLong(value));
    	else if (type.equals("float"))
            return new FloatValue(Float.parseFloat(value));
    	else if (type.equals("double"))
            return new DoubleValue(Double.parseDouble(value));
    	else
        	throw new InternalFailureException("unepected value type " + type);
    }

    /**
     * Yields the storage value model of the returned value of a method.
     * If the method returns void, its returned value is irrelevant and we fix it to {@code null}.
     * 
     * @param request the request that calls the method
     * @param returnedValue the value returned by the method
     * @return the
     */
    public static StorageValueModel modelOfValueReturned(MethodCallTransactionRequestModel request, StorageValue returnedValue) {
    	if (request.method.returnType == null && returnedValue == null)
    		return null;
    	else if (request.method.returnType == null)
    		throw new InternalFailureException("unexpected non-null return value for void method");
    	else if (returnedValue == null)
    		throw new InternalFailureException("unexpected null return value for non-void method");
    	else
    		return new StorageValueModel(returnedValue);
    }
}