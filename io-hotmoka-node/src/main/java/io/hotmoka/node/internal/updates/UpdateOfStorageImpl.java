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
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfStorage;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.gson.UpdateJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The implementation of an update of a field of storage (reference) type.
 */
@Immutable
public final class UpdateOfStorageImpl extends UpdateOfFieldImpl implements UpdateOfStorage {
	final static byte SELECTOR = 16;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE_LEFT = 23;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT = 24;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE_KEY = 25;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE_VALUE = 26;
	final static byte SELECTOR_STORAGE_TREE_MAP_ROOT = 28;
	final static byte SELECTOR_EVENT_CREATOR = 31;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE = 33;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT = 34;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT = 35;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_ROOT = 36;

	/**
	 * The new value of the field.
	 */
	private final StorageReference value;

	/**
	 * Builds an update of a field of storage (reference) type.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfStorageImpl(StorageReference object, FieldSignature field, StorageReference value) {
		this(object, field, value, IllegalArgumentException::new);
	}

	/**
	 * Builds an update of a field of storage (reference) type from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @param value the assigned value
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public UpdateOfStorageImpl(UpdateJson json, StorageReference value) throws InconsistentJsonException {
		this(
			unmapObject(json),
			unmapField(json),
			value,
			InconsistentJsonException::new
		);
	}

	/**
	 * Unmarshals an update of a field of storage (reference) type from the given context.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @throws IOException if the unmarshalling failed
	 */
	public UpdateOfStorageImpl(UnmarshallingContext context, byte selector) throws IOException {
		this(
			StorageReferenceImpl.fromWithoutSelector(context),
			unmarshalField(context, selector),
			StorageReferenceImpl.fromWithoutSelector(context),
			IOException::new
		);
	}

	/**
	 * Builds an update of a field of storage (reference) type.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> UpdateOfStorageImpl(StorageReference object, FieldSignature field, StorageReference value, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(object, field, onIllegalArgs);
	
		this.value = Objects.requireNonNull(value, "value cannot be null", onIllegalArgs);
	}

	private static FieldSignature unmarshalField(UnmarshallingContext context, int selector) throws IOException {
		switch (selector) {
		case SELECTOR: return FieldSignatures.from(context);
		case SELECTOR_STORAGE_TREE_MAP_NODE_LEFT: return FieldSignatures.STORAGE_TREE_MAP_NODE_LEFT_FIELD;
		case SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT: return FieldSignatures.STORAGE_TREE_MAP_NODE_RIGHT_FIELD;
		case SELECTOR_STORAGE_TREE_MAP_NODE_KEY: return FieldSignatures.STORAGE_TREE_MAP_NODE_KEY_FIELD;
		case SELECTOR_STORAGE_TREE_MAP_NODE_VALUE: return FieldSignatures.STORAGE_TREE_MAP_NODE_VALUE_FIELD;
		case SELECTOR_STORAGE_TREE_INTMAP_ROOT: return FieldSignatures.STORAGE_TREE_INTMAP_ROOT_FIELD;
		case SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE: return FieldSignatures.STORAGE_TREE_INTMAP_NODE_VALUE_FIELD;
		case SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT: return FieldSignatures.STORAGE_TREE_INTMAP_NODE_LEFT_FIELD;
		case SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT: return FieldSignatures.STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD;
		case SELECTOR_STORAGE_TREE_MAP_ROOT: return FieldSignatures.STORAGE_TREE_MAP_ROOT_FIELD;
		case SELECTOR_EVENT_CREATOR: return FieldSignatures.EVENT_CREATOR_FIELD;
		default: throw new IllegalArgumentException("Unexpected selector " + selector + " for a storage field update");
		}
	}

	@Override
	public StorageReference getValue() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfStorage uos && super.equals(other) && uos.getValue().equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public boolean isEager() {
		return false;
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfStorageImpl) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (FieldSignatures.EVENT_CREATOR_FIELD.equals(field)) {
			context.writeByte(SELECTOR_EVENT_CREATOR);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_MAP_ROOT_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_ROOT);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_INTMAP_ROOT_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_ROOT);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_MAP_NODE_LEFT_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE_LEFT);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_MAP_NODE_RIGHT_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_MAP_NODE_KEY_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE_KEY);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_MAP_NODE_VALUE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE_VALUE);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_INTMAP_NODE_VALUE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_INTMAP_NODE_LEFT_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else if (FieldSignatures.STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT);
			intoWithoutField(context);
			value.intoWithoutSelector(context);
		}
		else {
			context.writeByte(SELECTOR);
			super.into(context);
			value.intoWithoutSelector(context);
		}
	}
}