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
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfBigInteger;
import io.hotmoka.node.api.updates.UpdateOfBoolean;
import io.hotmoka.node.api.updates.UpdateOfByte;
import io.hotmoka.node.api.updates.UpdateOfChar;
import io.hotmoka.node.api.updates.UpdateOfDouble;
import io.hotmoka.node.api.updates.UpdateOfFloat;
import io.hotmoka.node.api.updates.UpdateOfInt;
import io.hotmoka.node.api.updates.UpdateOfLong;
import io.hotmoka.node.api.updates.UpdateOfShort;
import io.hotmoka.node.api.updates.UpdateOfStorage;
import io.hotmoka.node.api.updates.UpdateOfString;
import io.hotmoka.node.api.updates.UpdateToNull;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.json.UpdateDecoder;
import io.hotmoka.node.internal.json.UpdateEncoder;
import io.hotmoka.node.internal.json.UpdateJson;
import io.hotmoka.node.internal.updates.AbstractUpdate;
import io.hotmoka.node.internal.updates.ClassTagImpl;
import io.hotmoka.node.internal.updates.UpdateOfBigIntegerImpl;
import io.hotmoka.node.internal.updates.UpdateOfBooleanImpl;
import io.hotmoka.node.internal.updates.UpdateOfByteImpl;
import io.hotmoka.node.internal.updates.UpdateOfCharImpl;
import io.hotmoka.node.internal.updates.UpdateOfDoubleImpl;
import io.hotmoka.node.internal.updates.UpdateOfFloatImpl;
import io.hotmoka.node.internal.updates.UpdateOfIntImpl;
import io.hotmoka.node.internal.updates.UpdateOfLongImpl;
import io.hotmoka.node.internal.updates.UpdateOfShortImpl;
import io.hotmoka.node.internal.updates.UpdateOfStorageImpl;
import io.hotmoka.node.internal.updates.UpdateOfStringImpl;
import io.hotmoka.node.internal.updates.UpdateToNullImpl;

/**
 * Providers of updates.
 */
public abstract class Updates {

	private Updates() {}

	/**
	 * Yields an update for the class tag of an object.
	 * 
	 * @param object the storage reference of the object whose class name is set
	 * @param clazz the class of the object
	 * @param jar the reference to the transaction that installed the jar from which the class was resolved
	 * @return the update
	 */
	public static ClassTag classTag(StorageReference object, ClassType clazz, TransactionReference jar) {
		return new ClassTagImpl(object, clazz, jar);
	}

	/**
	 * Yields an update of a {@link java.math.BigInteger} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfBigInteger ofBigInteger(StorageReference object, FieldSignature field, BigInteger value) {
		return new UpdateOfBigIntegerImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@code boolean} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfBoolean ofBoolean(StorageReference object, FieldSignature field, boolean value) {
		return new UpdateOfBooleanImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@code byte} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfByte ofByte(StorageReference object, FieldSignature field, byte value) {
		return new UpdateOfByteImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@code char} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfChar ofChar(StorageReference object, FieldSignature field, char value) {
		return new UpdateOfCharImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@code short} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfShort ofShort(StorageReference object, FieldSignature field, short value) {
		return new UpdateOfShortImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@code int} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfInt ofInt(StorageReference object, FieldSignature field, int value) {
		return new UpdateOfIntImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@code long} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfLong ofLong(StorageReference object, FieldSignature field, long value) {
		return new UpdateOfLongImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@code float} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfFloat ofFloat(StorageReference object, FieldSignature field, float value) {
		return new UpdateOfFloatImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@code double} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfDouble ofDouble(StorageReference object, FieldSignature field, double value) {
		return new UpdateOfDoubleImpl(object, field, value);
	}

	/**
	 * Yields an update of a {@link java.lang.String} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfString ofString(StorageReference object, FieldSignature field, String value) {
		return new UpdateOfStringImpl(object, field, value);
	}

	/**
	 * Yields an update of a field of storage (reference) type.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @return the update
	 */
	public static UpdateOfStorage ofStorage(StorageReference object, FieldSignature field, StorageReference value) {
		return new UpdateOfStorageImpl(object, field, value);
	}

	/**
	 * Yields an update of a field to {@code null}.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param eager true if and only if the update is eager
	 * @return the update
	 */
	public static UpdateToNull toNull(StorageReference object, FieldSignature field, boolean eager) {
		return new UpdateToNullImpl(object, field, eager);
	}

	/**
	 * Yields the update unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the update
	 * @throws IOException if the update cannot be marshalled
     */
	public static Update from(UnmarshallingContext context) throws IOException {
		return AbstractUpdate.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends UpdateEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends UpdateDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * JSON representation.
     */
    public static class Json extends UpdateJson {

    	/**
    	 * Creates the JSON representation for the given update.
    	 * 
    	 * @param update the update
    	 */
    	public Json(Update update) {
    		super(update);
    	}
    }
}