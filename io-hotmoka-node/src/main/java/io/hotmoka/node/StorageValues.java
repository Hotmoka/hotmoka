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

package io.hotmoka.node;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.ByteValue;
import io.hotmoka.node.api.values.CharValue;
import io.hotmoka.node.api.values.DoubleValue;
import io.hotmoka.node.api.values.FloatValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.ShortValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.internal.gson.StorageValueDecoder;
import io.hotmoka.node.internal.gson.StorageValueEncoder;
import io.hotmoka.node.internal.gson.StorageValueJson;
import io.hotmoka.node.internal.values.AbstractStorageValue;
import io.hotmoka.node.internal.values.BigIntegerValueImpl;
import io.hotmoka.node.internal.values.BooleanValueImpl;
import io.hotmoka.node.internal.values.ByteValueImpl;
import io.hotmoka.node.internal.values.CharValueImpl;
import io.hotmoka.node.internal.values.DoubleValueImpl;
import io.hotmoka.node.internal.values.FloatValueImpl;
import io.hotmoka.node.internal.values.IntValueImpl;
import io.hotmoka.node.internal.values.LongValueImpl;
import io.hotmoka.node.internal.values.NullValueImpl;
import io.hotmoka.node.internal.values.ShortValueImpl;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.node.internal.values.StringValueImpl;

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
		return new BigIntegerValueImpl(value, IllegalArgumentException::new);
	}

	/**
	 * Yields the storage value corresponding to the given {@code long} value.
	 * 
	 * @param value the {@code long} value
	 * @return the corresponding storage value
	 */
	public static BigIntegerValue bigIntegerOf(long value) {
		return bigIntegerOf(BigInteger.valueOf(value));
	}

	/**
	 * Yields the storage value corresponding to the given string.
	 * 
	 * @param value the string
	 * @return the corresponding storage value
	 */
	public static StringValue stringOf(String value) {
		return new StringValueImpl(value, IllegalArgumentException::new);
	}

	/**
	 * Yields a storage reference from its transaction reference and progressive.
	 * 
	 * @param transaction the transaction that created the object
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 * @return the storage reference
	 */
	public static StorageReference reference(TransactionReference transaction, BigInteger progressive) {
		return new StorageReferenceImpl(transaction, progressive, IllegalArgumentException::new);
	}

	/**
	 * Yields a storage reference from its string representation.
	 * 
	 * @param s the string representation
	 * @return the storage reference
	 */
	public static StorageReference reference(String s) {
		return new StorageReferenceImpl(s, IllegalArgumentException::new);
	}

	/**
	 * Yields a storage value from the given string and of the given type.
	 * 
	 * @param s the string; use "null" (without quotes) for {@code null}
	 * @param type the type of the storage value
	 * @return the resulting storage value
	 */
	public static StorageValue of(String s, StorageType type) {
		return AbstractStorageValue.of(s, type);
	}

	/**
	 * Unmarshals a value from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the value
	 * @throws IOException if the value could not be unmarshalled
	 */
	public static StorageValue from(UnmarshallingContext context) throws IOException {
		return AbstractStorageValue.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends StorageValueEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends StorageValueDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends StorageValueJson {

    	/**
    	 * Creates the Json representation for the given storage value.
    	 * 
    	 * @param value the storage value
    	 */
    	public Json(StorageValue value) {
    		super(value);
    	}
    }
}