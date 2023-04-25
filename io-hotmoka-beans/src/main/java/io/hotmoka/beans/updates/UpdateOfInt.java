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

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * An update of a field states that an integer field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfInt extends UpdateOfField {
	final static byte SELECTOR = 20;
	final static byte SELECTOR_SMALL = 21;
	final static byte SELECTOR_VERY_SMALL = 22;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE_SIZE = 27;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE = 29;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY = 30;

	/**
	 * The new value of the field.
	 */
	public final int value;

	/**
	 * Builds an update of an {@code int} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfInt(StorageReference object, FieldSignature field, int value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return new IntValue(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfInt && super.equals(other) && ((UpdateOfInt) other).value == value;
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
			return Integer.compare(value, ((UpdateOfInt) other).value);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()));
	}

	@Override
	public void into(MarshallingContext context) {
		if (FieldSignature.STORAGE_TREE_MAP_NODE_SIZE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE_SIZE);
			intoWithoutField(context);
			context.writeCompactInt(value);
		}
		else if (FieldSignature.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD.equals(field)) {
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE);
			intoWithoutField(context);
			context.writeCompactInt(value);
		}
		else if (FieldSignature.STORAGE_TREE_INTMAP_NODE_KEY_FIELD.equals(field)) {
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