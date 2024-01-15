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

package io.hotmoka.beans.updates;

import java.io.IOException;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * An update of a field states that the field of storage type
 * of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfStorage extends UpdateOfField {
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
	public final StorageReference value;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfStorage(StorageReference object, FieldSignature field, StorageReference value) {
		super(object, field);

		this.value = Objects.requireNonNull(value, "value cannot be null");
	}

	@Override
	public StorageValue getValue() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfStorage uos && super.equals(other) && uos.value.equals(value);
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
			return value.compareTo(((UpdateOfStorage) other).value);
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