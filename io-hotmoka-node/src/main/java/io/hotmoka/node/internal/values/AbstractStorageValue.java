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

import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.json.StorageValueJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Partial implementation of a value that can be stored in the store of a Hotmoka node,
 * passed as argument or returned between the outside world and the node.
 */
public abstract class AbstractStorageValue extends AbstractMarshallable implements StorageValue {

	/**
	 * Yields a storage value from the given string and of the given type.
	 * 
	 * @param s the string; use "null" (without quotes) for {@code null}
	 * @param type the type of the storage value
	 * @return the resulting storage value
	 */
	public static StorageValue of(String s, StorageType type)  {
		Objects.requireNonNull(s, "s cannot be null", IllegalArgumentException::new);

		if (Objects.requireNonNull(type, "type cannot be null", IllegalArgumentException::new) == StorageTypes.BOOLEAN)
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
		else if ((type instanceof ClassType || StorageTypes.BIG_INTEGER.equals(type) || StorageTypes.STRING.equals(type)) && "null".equals(s))
			return StorageValues.NULL;
		else if (StorageTypes.BIG_INTEGER.equals(type)) {
			try {
				return StorageValues.bigIntegerOf(new BigInteger(s));
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("Cannot transform the string into a big integer", e);
			}
		}
		else if (StorageTypes.STRING.equals(type))
			return StorageValues.stringOf(s);
		else if (type instanceof ClassType) {
			if (s.startsWith("\"") && s.endsWith("\""))
				return StorageValues.stringOf(s.substring(1, s.length() - 1));
			else
				return StorageValues.reference(s);
		}

		throw new IllegalArgumentException("Cannot transform the string into a storage value");
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
		case BigIntegerValueImpl.SELECTOR: return new BigIntegerValueImpl(context);
		case BooleanValueImpl.SELECTOR_TRUE:
		case BooleanValueImpl.SELECTOR_FALSE: return BooleanValueImpl.from(context, selector);
		case ByteValueImpl.SELECTOR: return StorageValues.byteOf(context.readByte());
		case CharValueImpl.SELECTOR: return StorageValues.charOf(context.readChar());
		case DoubleValueImpl.SELECTOR: return StorageValues.doubleOf(context.readDouble());
		case FloatValueImpl.SELECTOR: return StorageValues.floatOf(context.readFloat());
		case IntValueImpl.SELECTOR: return StorageValues.intOf(context.readInt());
		case LongValueImpl.SELECTOR: return StorageValues.longOf(context.readLong());
		case NullValueImpl.SELECTOR: return StorageValues.NULL;
		case ShortValueImpl.SELECTOR: return StorageValues.shortOf(context.readShort());
		case StorageReferenceImpl.SELECTOR: return StorageReferenceImpl.fromWithoutSelector(context);
		case StringValueImpl.SELECTOR_EMPTY_STRING:
		case StringValueImpl.SELECTOR: return new StringValueImpl(context, selector);
		default: // small integers receive an optimized representation
			if (selector < 0)
				return StorageValues.intOf((selector + 256) - IntValueImpl.SELECTOR - 1);
			else
				return StorageValues.intOf(selector - IntValueImpl.SELECTOR - 1);
		}
	}

	public static StorageValue from(StorageValueJson json) throws InconsistentJsonException {
		var bigIntegerValue = json.getBigIntegerValue();
		if (bigIntegerValue != null)
			return new BigIntegerValueImpl(json);

		var booleanValue = json.getBooleanValue();
		if (booleanValue != null)
			return BooleanValueImpl.from(json);

		var byteValue = json.getByteValue();
		if (byteValue != null)
			return StorageValues.byteOf(byteValue);

		var charValue = json.getCharValue();
		if (charValue != null)
			return StorageValues.charOf(charValue);

		var doubleValue = json.getDoubleValue();
		if (doubleValue != null)
			return StorageValues.doubleOf(doubleValue);

		var floatValue = json.getFloatValue();
		if (floatValue != null)
			return StorageValues.floatOf(floatValue);

		var intValue = json.getIntValue();
		if (intValue != null)
			return StorageValues.intOf(intValue);

		var longValue = json.getLongValue();
		if (longValue != null)
			return StorageValues.longOf(longValue);

		if (json.isNullValue())
			return StorageValues.NULL;

		var shortValue = json.getShortValue();
		if (shortValue != null)
			return StorageValues.shortOf(shortValue);

		if (json.getTransaction() != null || json.getProgressive() != null)
			return new StorageReferenceImpl(json);

		var stringValue = json.getStringValue();
		if (stringValue != null)
			return new StringValueImpl(json);

		throw new InconsistentJsonException("Illegal storage value JSON");
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
	public <E extends Exception> BigInteger asReturnedBigInteger(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a BigInteger, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> boolean asBoolean(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> boolean asReturnedBoolean(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a boolean, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> byte asByte(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> byte asReturnedByte(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a byte, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> char asChar(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> char asReturnedChar(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a char, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> double asDouble(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> double asReturnedDouble(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a double, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> float asFloat(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> float asReturnedFloat(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a float, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> int asInt(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> int asReturnedInt(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return an int, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> long asLong(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> long asReturnedLong(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a long, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> short asShort(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> short asReturnedShort(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a short, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> StorageReference asReference(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> StorageReference asReturnedReference(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a reference, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	public <E extends Exception> String asString(Function<StorageValue, ? extends E> exception) throws E {
		throw exception.apply(this);  // subclasses may redefine
	}

	@Override
	public <E extends Exception> String asReturnedString(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		throw exception.apply(method + " should return a String, not a " + getClass().getName());  // subclasses may redefine
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) {
		return NodeMarshallingContexts.of(os);
	}
}