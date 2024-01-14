/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.network.values;

import java.math.BigInteger;

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.ByteValue;
import io.hotmoka.beans.api.values.CharValue;
import io.hotmoka.beans.api.values.DoubleValue;
import io.hotmoka.beans.api.values.EnumValue;
import io.hotmoka.beans.api.values.FloatValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.ShortValue;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.requests.MethodCallTransactionRequestModel;

/**
 * The model of a storage value.
 */
public class StorageValueModel {
	private static final String BIGINTEGER_NAME = BigInteger.class.getName();
	private static final String STRING_NAME = String.class.getName();

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
    		throw new RuntimeException("Unexpected null storage value");
    	else if (parent instanceof StorageReference) {
    		value = null;
    		reference = new StorageReferenceModel((StorageReference) parent);
    		type = "reference";
    		enumElementName = null;
    	}
    	else if (parent == StorageValues.NULL) {
    		value = null;
    		reference = null;
    		type = "reference";
    		enumElementName = null;
    	}
    	else if (parent instanceof EnumValue) {
    		EnumValue parentAsEnumValue = (EnumValue) parent;
    		value = null;
    		reference = null;
    		type = parentAsEnumValue.getEnumClassName();
    		enumElementName = parentAsEnumValue.getName();
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
    		throw new RuntimeException("unexpected storage value of class " + parent.getClass().getName());
    }

	public StorageValueModel() {}

	/**
     * Yields the storage value corresponding to this value.
     * @return the storage value
     */
    public StorageValue toBean() {
    	if (enumElementName != null)
			return StorageValues.enumElementOf(type, enumElementName);
    	else if (type.equals("reference"))
    		if (reference == null)
    			return StorageValues.NULL;
            else
            	return reference.toBean();
    	else if (value == null)
    		throw new RuntimeException("unexpected null value");
    	else if (type.equals(BIGINTEGER_NAME))
			return StorageValues.bigIntegerOf(new BigInteger(value));
		else if (type.equals(STRING_NAME))
			return StorageValues.stringOf(value);
		else if (type.equals("boolean"))
            return StorageValues.booleanOf(Boolean.parseBoolean(value));
    	else if (type.equals("byte"))
            return StorageValues.byteOf(Byte.parseByte(value));
    	else if (type.equals("char"))
            return StorageValues.charOf(value.charAt(0));
    	else if (type.equals("short"))
            return StorageValues.shortOf(Short.parseShort(value));
    	else if (type.equals("int"))
            return StorageValues.intOf(Integer.parseInt(value));
    	else if (type.equals("long"))
            return StorageValues.longOf(Long.parseLong(value));
    	else if (type.equals("float"))
            return StorageValues.floatOf(Float.parseFloat(value));
    	else if (type.equals("double"))
            return StorageValues.doubleOf(Double.parseDouble(value));
    	else
        	throw new RuntimeException("unexpected value type " + type);
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
    		throw new RuntimeException("unexpected non-null return value for void method");
    	else if (returnedValue == null)
    		throw new RuntimeException("unexpected null return value for non-void method");
    	else
    		return new StorageValueModel(returnedValue);
    }
}