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

package io.hotmoka.node.internal.updates;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfInt;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.json.UpdateJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The implementation of an update of a field of type {@code int}.
 */
@Immutable
public final class UpdateOfIntImpl extends UpdateOfFieldImpl implements UpdateOfInt {
	final static byte SELECTOR = 20;
	final static byte SELECTOR_SMALL = 21;
	final static byte SELECTOR_VERY_SMALL = 22;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE_SIZE = 27;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE = 29;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY = 30;

	/**
	 * The new value of the field.
	 */
	private final int value;

	/**
	 * Builds an update of an {@code int} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfIntImpl(StorageReference object, FieldSignature field, int value) {
		this(object, field, value, IllegalArgumentException::new);
	}

	/**
	 * Builds an update of an {@code int} field from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @param value the assigned value
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public UpdateOfIntImpl(UpdateJson json, int value) throws InconsistentJsonException {
		this(
			unmapObject(json),
			unmapField(json),
			value,
			InconsistentJsonException::new
		);
	}

	/**
	 * Unmarshals an update of an {@code int} field from the given context.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @throws IOException if the unmarshalling failed
	 */
	public UpdateOfIntImpl(UnmarshallingContext context, byte selector) throws IOException {
		this(
			StorageReferenceImpl.fromWithoutSelector(context),
			unmarshalField(context, selector),
			unmarshalValue(context, selector),
			IOException::new
		);
	}

	/**
	 * Builds an update of an {@code int} field.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> UpdateOfIntImpl(StorageReference object, FieldSignature field, int value, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(object, field, onIllegalArgs);
	
		this.value = value;
	}

	private static FieldSignature unmarshalField(UnmarshallingContext context, int selector) throws IOException {
		switch (selector) {
		case SELECTOR:
		case SELECTOR_SMALL:
		case SELECTOR_VERY_SMALL: return FieldSignatures.from(context);
		case SELECTOR_STORAGE_TREE_MAP_NODE_SIZE: return FieldSignatures.STORAGE_TREE_MAP_NODE_SIZE_FIELD;
		case SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE: return FieldSignatures.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD;
		case SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY: return FieldSignatures.STORAGE_TREE_INTMAP_NODE_KEY_FIELD;
		default: throw new IllegalArgumentException("Unexpected selector " + selector + " for an int field update");
		}
	}

	private static int unmarshalValue(UnmarshallingContext context, int selector) throws IOException {
		switch (selector) {
		case SELECTOR: return context.readInt();
		case SELECTOR_SMALL: return context.readShort();
		case SELECTOR_VERY_SMALL: return context.readByte();
		case SELECTOR_STORAGE_TREE_MAP_NODE_SIZE:
		case SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE:
		case SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY: return context.readCompactInt();
		default: throw new IllegalArgumentException("Unexpected selector " + selector + " for an int field update");
		}
	}

	@Override
	public IntValue getValue() {
		return StorageValues.intOf(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfInt uoi && super.equals(other) && uoi.getValue().getValue() == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value;
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Integer.compare(value, ((UpdateOfIntImpl) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (FieldSignatures.STORAGE_TREE_MAP_NODE_SIZE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE_SIZE);
			intoWithoutField(context);
			context.writeCompactInt(value);
		}
		else if (FieldSignatures.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE);
			intoWithoutField(context);
			context.writeCompactInt(value);
		}
		else if (FieldSignatures.STORAGE_TREE_INTMAP_NODE_KEY_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY);
			intoWithoutField(context);
			context.writeCompactInt(value);
		}
		else {
			boolean isSmall = ((short) value) == value;
			boolean isVerySmall = ((byte) value) == value;

			if (isVerySmall)
				context.writeByte(SELECTOR_VERY_SMALL);
			else if (isSmall)
				context.writeByte(SELECTOR_SMALL);
			else
				context.writeByte(SELECTOR);

			super.into(context);

			if (isVerySmall)
				context.writeByte((byte) value);
			else if (isSmall)
				context.writeShort((short) value);
			else
				context.writeInt(value);
		}
	}
}