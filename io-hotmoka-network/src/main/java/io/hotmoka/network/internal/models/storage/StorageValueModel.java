package io.hotmoka.network.internal.models.storage;

import java.math.BigInteger;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;

public class StorageValueModel {
    private String value;
    private String type;
    private StorageReferenceModel reference;

    public StorageValueModel() {}

    public StorageValueModel(StorageValue input) {
    	if (input == null)
    		throw new InternalFailureException("unexpected null storage value");

    	value = input.toString();

    	if (input instanceof BooleanValue)
    		type = "boolean";
    	else if (input instanceof ByteValue)
    		type = "byte";
    	else if (input instanceof CharValue)
    		type = "char";
    	else if (input instanceof ShortValue)
    		type = "short";
    	else if (input instanceof IntValue)
    		type = "int";
    	else if (input instanceof LongValue)
    		type = "long";
    	else if (input instanceof FloatValue)
    		type = "float";
    	else if (input instanceof DoubleValue)
    		type = "double";
    	else if (input instanceof BigIntegerValue)
    		type = "java.math.BigInteger";
    	else if (input instanceof StringValue)
    		type = "java.lang.String";
    	else if (input instanceof NullValue)
    		type = "null";
    	else if (input instanceof StorageReference) {
    		type = "reference";
    		reference = new StorageReferenceModel((StorageReference) input);
    	}
    	// TODO deal with enums
    	else
    		throw new InternalFailureException("unexpected storage value of type " + input.getClass().getName());
    }

    public StorageValue toBean() {
    	if (type == null)
    		throw new InternalFailureException("unexpected null storage value type");

    	switch (type) {
    	case "boolean":
            return new BooleanValue(Boolean.parseBoolean(value));
        case "byte":
            return new ByteValue(Byte.parseByte(value));
        case "char":
            return new CharValue(value.charAt(0));
        case "short":
            return new ShortValue(Short.parseShort(value));
        case "int":
            return new IntValue(Integer.parseInt(value));
        case "long":
            return new LongValue(Long.parseLong(value));
        case "float":
            return new FloatValue(Float.parseFloat(value));
        case "double":
            return new DoubleValue(Double.parseDouble(value));
        case "java.math.BigInteger":
            return new BigIntegerValue(new BigInteger(value));
        case "java.lang.String":
            return new StringValue(value);
        case "null":
            return NullValue.INSTANCE;
        case "reference": {
            if (reference == null)
            	throw new InternalFailureException("unexpected null reference");
            else
            	return reference.toBean();
        }
        // TODO: deal with enums
        default:
        	throw new InternalFailureException("unepected value type " + type);
    	}
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setReference(StorageReferenceModel reference) {
        this.reference = reference;
    }

    public void setValue(String value) {
        this.value = value;
    }
}