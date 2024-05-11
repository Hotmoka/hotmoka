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

package io.hotmoka.node.internal.values;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.function.Function;

import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.marshalling.NodeMarshallingContext;

/**
 * Partial implementation of a value that can be stored in the blockchain,
 * passed as argument to an entry or returned from an entry.
 */
public abstract class AbstractStorageValue extends AbstractMarshallable implements StorageValue {

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
		else if (type == StorageTypes.CHAR && s.length() == 1)
			return StorageValues.charOf(s.charAt(0));
		else if (type == StorageTypes.SHORT)
			return StorageValues.shortOf(Short.parseShort(s));
		else if (type == StorageTypes.INT)
			return StorageValues.intOf(Integer.parseInt(s));
		else if (type == StorageTypes.LONG)
			return StorageValues.longOf(Long.parseLong(s));
		else if (type == StorageTypes.FLOAT)
			return StorageValues.floatOf(Float.parseFloat(s));
		else if (type == StorageTypes.DOUBLE)
			return StorageValues.doubleOf(Double.parseDouble(s));
		else if ((type instanceof ClassType || type.equals(StorageTypes.BIG_INTEGER) || StorageTypes.STRING.equals(type)) && "null".equals(s))
			return StorageValues.NULL;
		else if (StorageTypes.BIG_INTEGER.equals(type))
			return StorageValues.bigIntegerOf(new BigInteger(s));
		else if (StorageTypes.STRING.equals(type))
			return StorageValues.stringOf(s);
		else if (type instanceof ClassType)
			if (s.contains("#"))
				return StorageValues.reference(s);
			else {
				int lastDot = s.lastIndexOf('.');
				if (lastDot > 0)
					return StorageValues.enumElementOf(s.substring(0, lastDot), s.substring(lastDot + 1));
			}

		throw new IllegalArgumentException("Cannot transform " + s + " into a storage value");
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
		case BigIntegerValueImpl.SELECTOR: return StorageValues.bigIntegerOf(context.readBigInteger());
		case BooleanValueImpl.SELECTOR_TRUE: return StorageValues.TRUE;
		case BooleanValueImpl.SELECTOR_FALSE: return StorageValues.FALSE;
		case ByteValueImpl.SELECTOR: return StorageValues.byteOf(context.readByte());
		case CharValueImpl.SELECTOR: return StorageValues.charOf(context.readChar());
		case DoubleValueImpl.SELECTOR: return StorageValues.doubleOf(context.readDouble());
		case EnumValueImpl.SELECTOR: return StorageValues.enumElementOf(context.readStringUnshared(), context.readStringUnshared());
		case FloatValueImpl.SELECTOR: return StorageValues.floatOf(context.readFloat());
		case IntValueImpl.SELECTOR: return StorageValues.intOf(context.readInt());
		case LongValueImpl.SELECTOR: return StorageValues.longOf(context.readLong());
		case NullValueImpl.SELECTOR: return StorageValues.NULL;
		case ShortValueImpl.SELECTOR: return StorageValues.shortOf(context.readShort());
		case StorageReferenceImpl.SELECTOR: return StorageValues.referenceWithoutSelectorFrom(context);
		case StringValueImpl.SELECTOR_EMPTY_STRING: return StorageValues.stringOf("");
		case StringValueImpl.SELECTOR: return StorageValues.stringOf(context.readStringUnshared());
		default:
			if (selector < 0)
				return StorageValues.intOf((selector + 256) - IntValueImpl.SELECTOR - 1);
			else
				return StorageValues.intOf(selector - IntValueImpl.SELECTOR - 1);
		}
	}

	@Override
	public int compareTo(StorageValue other) {
		return getClass().getName().compareTo(other.getClass().getName());
	}

	@Override
	public <E extends Exception> BigInteger asBigInteger(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this); // subclasses may redefine
	}

	@Override
	public <E extends Exception> boolean asBoolean(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> byte asByte(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> char asChar(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> double asDouble(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> float asFloat(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> int asInt(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> long asLong(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> short asShort(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> StorageReference asReference(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> String asString(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new NodeMarshallingContext(os);
	}
}