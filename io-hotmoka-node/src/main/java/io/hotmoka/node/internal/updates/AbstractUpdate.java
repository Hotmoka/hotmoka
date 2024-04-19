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
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.NodeMarshallingContexts;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.Updates;

/**
 * Shared implementation of an update.
 */
@Immutable
public abstract class AbstractUpdate extends AbstractMarshallable implements Update {

	/**
	 * The storage reference of the object whose field is modified.
	 */
	private final StorageReference object;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 */
	protected AbstractUpdate(StorageReference object) {
		this.object = Objects.requireNonNull(object, "object cannot be null");
	}

	@Override
	public final StorageReference getObject() {
		return object;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Update u && u.getObject().equals(object);
	}

	@Override
	public int hashCode() {
		return object.hashCode();
	}

	@Override
	public int compareTo(Update other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return object.compareTo(other.getObject());
	}

	@Override
	public boolean isEager() {
		return true; // subclasses may redefine
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		object.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals an update from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the update
	 * @throws IOException if the update cannot be unmarshalled
	 */
	public static Update from(UnmarshallingContext context) throws IOException {
		var selector = context.readByte();
		switch (selector) {
		case ClassTagImpl.SELECTOR: {
			try {
				return Updates.classTag(StorageValues.referenceWithoutSelectorFrom(context), (ClassType) StorageTypes.from(context), TransactionReferences.from(context));
			}
			catch (ClassCastException e) {
				throw new IOException("Failed unmrshalling a class tag", e);
			}
		}
		case UpdateOfBigIntegerImpl.SELECTOR_BALANCE: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.BALANCE_FIELD, context.readBigInteger());
		case UpdateOfBigIntegerImpl.SELECTOR_GAS_PRICE: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.GENERIC_GAS_STATION_GAS_PRICE_FIELD, context.readBigInteger());
		case UpdateOfBigIntegerImpl.SELECTOR_UBI_VALUE: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.UNSIGNED_BIG_INTEGER_VALUE_FIELD, context.readBigInteger());
		case UpdateOfBigIntegerImpl.SELECTOR_RED_BALANCE: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.RED_BALANCE_FIELD, context.readBigInteger());
		case UpdateOfBigIntegerImpl.SELECTOR_RED_BALANCE_TO_ZERO: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.RED_BALANCE_FIELD, BigInteger.ZERO);
		case UpdateOfBigIntegerImpl.SELECTOR_BALANCE_TO_ZERO: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.BALANCE_FIELD, BigInteger.ZERO);
		case UpdateOfBigIntegerImpl.SELECTOR_NONCE_TO_ZERO: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.EOA_NONCE_FIELD, BigInteger.ZERO);
		case UpdateOfBigIntegerImpl.SELECTOR_NONCE: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.EOA_NONCE_FIELD, context.readBigInteger());
		case UpdateOfBigIntegerImpl.SELECTOR: return Updates.ofBigInteger(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readBigInteger());
		case UpdateOfBooleanImpl.SELECTOR_FALSE: return Updates.ofBoolean(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), false);
		case UpdateOfBooleanImpl.SELECTOR_TRUE: return Updates.ofBoolean(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), true);
		case UpdateOfByteImpl.SELECTOR: return Updates.ofByte(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readByte());
		case UpdateOfCharImpl.SELECTOR: return Updates.ofChar(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readChar());
		case UpdateOfDoubleImpl.SELECTOR: return Updates.ofDouble(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readDouble());
		case UpdateOfEnumImpl.SELECTOR_EAGER: return Updates.ofEnum(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readStringUnshared(), context.readStringUnshared(), true);
		case UpdateOfEnumImpl.SELECTOR_LAZY: return Updates.ofEnum(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readStringUnshared(), context.readStringUnshared(), false);
		case UpdateOfFloatImpl.SELECTOR: return Updates.ofFloat(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readFloat());
		case UpdateOfIntImpl.SELECTOR: return Updates.ofInt(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readInt());
		case UpdateOfIntImpl.SELECTOR_SMALL: return Updates.ofInt(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readShort());
		case UpdateOfIntImpl.SELECTOR_VERY_SMALL: return Updates.ofInt(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readByte());
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_MAP_NODE_SIZE: return Updates.ofInt(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_MAP_NODE_SIZE_FIELD, context.readCompactInt());
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE: return Updates.ofInt(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD, context.readCompactInt());
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY: return Updates.ofInt(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_KEY_FIELD, context.readCompactInt());
		case UpdateOfLongImpl.SELECTOR: return Updates.ofLong(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readLong());
		case UpdateOfShortImpl.SELECTOR: return Updates.ofShort(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readShort());
		case UpdateOfStorageImpl.SELECTOR: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_LEFT: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_MAP_NODE_LEFT_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_MAP_NODE_RIGHT_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_KEY: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_MAP_NODE_KEY_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_VALUE: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_MAP_NODE_VALUE_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_ROOT: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_INTMAP_ROOT_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_VALUE_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_LEFT_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_ROOT: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.STORAGE_TREE_MAP_ROOT_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStorageImpl.SELECTOR_EVENT_CREATOR: return Updates.ofStorage(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.EVENT_CREATOR_FIELD, StorageValues.referenceWithoutSelectorFrom(context));
		case UpdateOfStringImpl.SELECTOR_PUBLIC_KEY: return Updates.ofString(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.EOA_PUBLIC_KEY_FIELD, context.readStringUnshared());
		case UpdateOfStringImpl.SELECTOR: return Updates.ofString(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), context.readStringUnshared());
		case UpdateToNullImpl.SELECTOR_EAGER: return Updates.toNull(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), true);
		case UpdateToNullImpl.SELECTOR_LAZY: return Updates.toNull(StorageValues.referenceWithoutSelectorFrom(context), FieldSignatures.from(context), false);
		default: throw new IOException("Unexpected update selector: " + selector);
		}
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return NodeMarshallingContexts.of(os);
	}
}