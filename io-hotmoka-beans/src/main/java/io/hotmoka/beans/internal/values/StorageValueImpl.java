/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.beans.internal.values;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A value that can be stored in the blockchain, passed as argument to an entry
 * or returned from an entry.
 */
public abstract class StorageValueImpl extends AbstractMarshallable implements StorageValue {

	/**
	 * Yields a storage value from the given string and of the given type.
	 * 
	 * @param s the string; use "null" (without quotes) for null; use the fully-qualified
	 *        representation for enum's (such as "com.mycompany.MyEnum.CONSTANT")
	 * @param type the type of the storage value
	 * @return the resulting storage value
	 */
	public static StorageValue of(String s, StorageType type) {
		if (type == StorageTypes.BOOLEAN)
			return StorageValues.booleanOf(Boolean.parseBoolean(s));
		else if (type == StorageTypes.BYTE)
			return StorageValues.byteOf(Byte.parseByte(s));
		else if (type == StorageTypes.CHAR) {
			if (s.length() != 1)
				throw new IllegalArgumentException("the value is not a character");
			else
				return new CharValue(s.charAt(0));
		}
		else if (type == StorageTypes.SHORT)
			return new ShortValue(Short.parseShort(s));
		else if (type == StorageTypes.INT)
			return new IntValue(Integer.parseInt(s));
		else if (type == StorageTypes.LONG)
			return new LongValue(Long.parseLong(s));
		else if (type == StorageTypes.FLOAT)
			return new FloatValue(Float.parseFloat(s));
		else if (type == StorageTypes.DOUBLE)
			return new DoubleValue(Double.parseDouble(s));
		else if (StorageTypes.STRING.equals(type))
			return new StringValue(s);
		else if (StorageTypes.BIG_INTEGER.equals(type))
			return new BigIntegerValue(new BigInteger(s));
		else if ("null".equals(s))
			return NullValue.INSTANCE;
		else if (!s.contains("#")) {
			int lastDot = s.lastIndexOf('.');
			if (lastDot < 0)
				throw new IllegalArgumentException("Cannot interpret value " + s);
			else
				return new EnumValue(s.substring(0, lastDot), s.substring(lastDot + 1));
		}
		else
			return new StorageReference(s);
	}

	/**
	 * Factory method that unmarshals a value from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the value
	 * @throws IOException if the value could not be unmarshalled
	 */
	public static StorageValue from(UnmarshallingContext context) throws IOException {
		var selector = context.readByte();
		switch (selector) {
		case BigIntegerValue.SELECTOR: return new BigIntegerValue(context.readBigInteger());
		case BooleanValueImpl.SELECTOR_TRUE: return StorageValues.TRUE;
		case BooleanValueImpl.SELECTOR_FALSE: return StorageValues.FALSE;
		case ByteValueImpl.SELECTOR: return StorageValues.byteOf(context.readByte());
		case CharValue.SELECTOR: return new CharValue(context.readChar());
		case DoubleValue.SELECTOR: return new DoubleValue(context.readDouble());
		case EnumValue.SELECTOR: return new EnumValue(context.readStringUnshared(), context.readStringUnshared());
		case FloatValue.SELECTOR: return new FloatValue(context.readFloat());
		case IntValue.SELECTOR: return new IntValue(context.readInt());
		case LongValue.SELECTOR: return new LongValue(context.readLong());
		case NullValue.SELECTOR: return NullValue.INSTANCE;
		case ShortValue.SELECTOR: return new ShortValue(context.readShort());
		case StorageReference.SELECTOR: return StorageReference.from(context);
		case StringValue.SELECTOR_EMPTY_STRING: return new StringValue("");
		case StringValue.SELECTOR: return new StringValue(context.readStringUnshared());
		default:
			if (selector < 0)
				return new IntValue((selector + 256) - IntValue.SELECTOR - 1);
			else
				return new IntValue(selector - IntValue.SELECTOR - 1);
		}
	}

	@Override
	public int compareTo(StorageValue other) {
		return getClass().getName().compareTo(other.getClass().getName());
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}