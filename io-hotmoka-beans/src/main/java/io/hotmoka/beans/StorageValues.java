/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.ByteValue;
import io.hotmoka.beans.api.values.CharValue;
import io.hotmoka.beans.api.values.DoubleValue;
import io.hotmoka.beans.api.values.EnumValue;
import io.hotmoka.beans.api.values.FloatValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.NullValue;
import io.hotmoka.beans.api.values.ShortValue;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.beans.internal.gson.StorageTypeDecoder;
import io.hotmoka.beans.internal.gson.StorageTypeEncoder;
import io.hotmoka.beans.internal.gson.StorageTypeJson;
import io.hotmoka.beans.internal.values.BigIntegerValueImpl;
import io.hotmoka.beans.internal.values.BooleanValueImpl;
import io.hotmoka.beans.internal.values.ByteValueImpl;
import io.hotmoka.beans.internal.values.CharValueImpl;
import io.hotmoka.beans.internal.values.DoubleValueImpl;
import io.hotmoka.beans.internal.values.EnumValueImpl;
import io.hotmoka.beans.internal.values.FloatValueImpl;
import io.hotmoka.beans.internal.values.IntValueImpl;
import io.hotmoka.beans.internal.values.LongValueImpl;
import io.hotmoka.beans.internal.values.NullValueImpl;
import io.hotmoka.beans.internal.values.ShortValueImpl;
import io.hotmoka.beans.internal.values.StorageValueImpl;
import io.hotmoka.beans.internal.values.StringValueImpl;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of storage types.
 */
public abstract class StorageValues {

	private StorageValues() {}

	/**
	 * The true Boolean value.
	 */
	public final static BooleanValue TRUE = BooleanValueImpl.TRUE;

	/**
	 * The false Boolean value.
	 */
	public final static BooleanValue FALSE = BooleanValueImpl.FALSE;

	/**
	 * The {@code null} value.
	 */
	public final static NullValue NULL = NullValueImpl.NULL;

	/**
	 * Yields the storage value corresponding to the given boolean value.
	 * 
	 * @param value the boolean value
	 * @return the corresponding storage value
	 */
	public static BooleanValue booleanOf(boolean value) {
		return value ? TRUE : FALSE;
	}

	/**
	 * Yields the storage value corresponding to the given byte value.
	 * 
	 * @param value the byte value
	 * @return the corresponding storage value
	 */
	public static ByteValue byteOf(byte value) {
		return new ByteValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given character value.
	 * 
	 * @param value the character value
	 * @return the corresponding storage value
	 */
	public static CharValue charOf(char value) {
		return new CharValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given short value.
	 * 
	 * @param value the short value
	 * @return the corresponding storage value
	 */
	public static ShortValue shortOf(short value) {
		return new ShortValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given {@code int} value.
	 * 
	 * @param value the {@code int} value
	 * @return the corresponding storage value
	 */
	public static IntValue intOf(int value) {
		return new IntValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given long value.
	 * 
	 * @param value the long value
	 * @return the corresponding storage value
	 */
	public static LongValue longOf(long value) {
		return new LongValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given double value.
	 * 
	 * @param value the double value
	 * @return the corresponding storage value
	 */
	public static DoubleValue doubleOf(double value) {
		return new DoubleValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given {@code float} value.
	 * 
	 * @param value the {@code float} value
	 * @return the corresponding storage value
	 */
	public static FloatValue floatOf(float value) {
		return new FloatValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given big integer value.
	 * 
	 * @param value the big integer value
	 * @return the corresponding storage value
	 */
	public static BigIntegerValue bigIntegerOf(BigInteger value) {
		return new BigIntegerValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given {@code long} value.
	 * 
	 * @param value the {@code long} value
	 * @return the corresponding storage value
	 */
	public static BigIntegerValue bigIntegerOf(long value) {
		return new BigIntegerValueImpl(BigInteger.valueOf(value));
	}

	/**
	 * Yields the storage value corresponding to the given string.
	 * 
	 * @param value the string
	 * @return the corresponding storage value
	 */
	public static StringValue stringOf(String value) {
		return new StringValueImpl(value);
	}

	/**
	 * Yields the storage value corresponding to the given enumeration element.
	 * 
	 * @param enumClassName the name of the class of the enumeration
	 * @param name the name of the enumeration element
	 * @return the corresponding enumeration element value
	 */
	public static EnumValue enumElementOf(String enumClassName, String name) {
		return new EnumValueImpl(enumClassName, name);
	}

	/**
	 * Yields a storage value from the given string and of the given type.
	 * 
	 * @param s the string; use {@code "null"} for null; use the fully-qualified
	 *        representation for enum's (such as "com.mycompany.MyEnum.CONSTANT")
	 * @param type the type of the storage value
	 * @return the resulting storage value
	 */
	public static StorageValue of(String s, StorageType type) {
		return StorageValueImpl.of(s, type);
	}

	/**
	 * Factory method that unmarshals a value from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the value
	 * @throws IOException if the value could not be unmarshalled
	 */
	public static StorageValue from(UnmarshallingContext context) throws IOException {
		return StorageValueImpl.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends StorageTypeEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends StorageTypeDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends StorageTypeJson {

    	/**
    	 * Creates the Json representation for the given type.
    	 * 
    	 * @param type the type
    	 */
    	public Json(StorageType type) {
    		super(type);
    	}
    }
}