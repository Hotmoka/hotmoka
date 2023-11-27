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

package io.hotmoka.beans.values;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A value that can be stored in the blockchain, passed as argument to an entry
 * or returned from an entry.
 */
public abstract class StorageValue extends AbstractMarshallable implements Comparable<StorageValue> {

	/**
	 * Yields a storage value from the given string and of the given type.
	 * 
	 * @param s the string; use "null" (without quotes) for null; use the fully-qualified
	 *        representation for enum's (such as "com.mycompany.MyEnum.CONSTANT")
	 * @param type the type of the storage value
	 * @return the resulting storage value
	 */
	public static StorageValue of(String s, StorageType type) {
		if (type instanceof BasicTypes)
			switch ((BasicTypes) type) {
			case BOOLEAN: return new BooleanValue(Boolean.parseBoolean(s));
			case BYTE: return new ByteValue(Byte.parseByte(s));
			case CHAR: {
				if (s.length() != 1)
					throw new IllegalArgumentException("the value is not a character");
				else
					return new CharValue(s.charAt(0));
			}
			case SHORT: return new ShortValue(Short.parseShort(s));
			case INT: return new IntValue(Integer.parseInt(s));
			case LONG: return new LongValue(Long.parseLong(s));
			case FLOAT: return new FloatValue(Float.parseFloat(s));
			default: return new DoubleValue(Double.parseDouble(s));
			}
		else if (ClassType.STRING.equals(type))
			return new StringValue(s);
		else if (ClassType.BIG_INTEGER.equals(type))
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
		case BooleanValue.SELECTOR_TRUE: return BooleanValue.TRUE;
		case BooleanValue.SELECTOR_FALSE: return BooleanValue.FALSE;
		case ByteValue.SELECTOR: return new ByteValue(context.readByte());
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
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}