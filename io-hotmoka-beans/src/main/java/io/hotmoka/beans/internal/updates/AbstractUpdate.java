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

package io.hotmoka.beans.internal.updates;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.internal.values.StorageReferenceImpl;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.UpdateOfBigInteger;
import io.hotmoka.beans.updates.UpdateOfBoolean;
import io.hotmoka.beans.updates.UpdateOfByte;
import io.hotmoka.beans.updates.UpdateOfChar;
import io.hotmoka.beans.updates.UpdateOfDouble;
import io.hotmoka.beans.updates.UpdateOfEnumEager;
import io.hotmoka.beans.updates.UpdateOfEnumLazy;
import io.hotmoka.beans.updates.UpdateOfFloat;
import io.hotmoka.beans.updates.UpdateOfInt;
import io.hotmoka.beans.updates.UpdateOfLong;
import io.hotmoka.beans.updates.UpdateOfShort;
import io.hotmoka.beans.updates.UpdateOfStorage;
import io.hotmoka.beans.updates.UpdateOfString;
import io.hotmoka.beans.updates.UpdateToNullEager;
import io.hotmoka.beans.updates.UpdateToNullLazy;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

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
	public static AbstractUpdate from(UnmarshallingContext context) throws IOException {
		var selector = context.readByte();
		switch (selector) {
		case ClassTag.SELECTOR: {
			try {
				return new ClassTag(StorageReferenceImpl.fromWithoutSelector(context), (ClassType) StorageTypes.from(context), TransactionReferences.from(context));
			}
			catch (ClassCastException e) {
				throw new IOException("Failed unmrshalling a class tag", e);
			}
		}
		case UpdateOfBigInteger.SELECTOR_BALANCE: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.BALANCE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR_GAS_PRICE: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.GENERIC_GAS_STATION_GAS_PRICE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR_UBI_VALUE: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.UNSIGNED_BIG_INTEGER_VALUE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR_RED_BALANCE: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.RED_BALANCE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR_RED_BALANCE_TO_ZERO: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.RED_BALANCE_FIELD, BigInteger.ZERO);
		case UpdateOfBigInteger.SELECTOR_BALANCE_TO_ZERO: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.BALANCE_FIELD, BigInteger.ZERO);
		case UpdateOfBigInteger.SELECTOR_NONCE_TO_ZERO: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.EOA_NONCE_FIELD, BigInteger.ZERO);
		case UpdateOfBigInteger.SELECTOR_NONCE: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.EOA_NONCE_FIELD, context.readBigInteger());
		case UpdateOfBigInteger.SELECTOR: return new UpdateOfBigInteger(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readBigInteger());
		case UpdateOfBoolean.SELECTOR_FALSE: return new UpdateOfBoolean(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), false);
		case UpdateOfBoolean.SELECTOR_TRUE: return new UpdateOfBoolean(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), true);
		case UpdateOfByte.SELECTOR: return new UpdateOfByte(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readByte());
		case UpdateOfChar.SELECTOR: return new UpdateOfChar(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readChar());
		case UpdateOfDouble.SELECTOR: return new UpdateOfDouble(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readDouble());
		case UpdateOfEnumEager.SELECTOR: return new UpdateOfEnumEager(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readStringUnshared(), context.readStringUnshared());
		case UpdateOfEnumLazy.SELECTOR: return new UpdateOfEnumLazy(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readStringUnshared(), context.readStringUnshared());		
		case UpdateOfFloat.SELECTOR: return new UpdateOfFloat(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readFloat());
		case UpdateOfInt.SELECTOR: return new UpdateOfInt(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readInt());
		case UpdateOfInt.SELECTOR_SMALL: return new UpdateOfInt(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readShort());
		case UpdateOfInt.SELECTOR_VERY_SMALL: return new UpdateOfInt(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readByte());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_MAP_NODE_SIZE: return new UpdateOfInt(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_MAP_NODE_SIZE_FIELD, context.readCompactInt());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE: return new UpdateOfInt(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD, context.readCompactInt());
		case UpdateOfInt.SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY: return new UpdateOfInt(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_KEY_FIELD, context.readCompactInt());
		case UpdateOfLong.SELECTOR: return new UpdateOfLong(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readLong());
		case UpdateOfShort.SELECTOR: return new UpdateOfShort(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readShort());
		case UpdateOfStorage.SELECTOR: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_LEFT: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_MAP_NODE_LEFT_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_MAP_NODE_RIGHT_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_KEY: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_MAP_NODE_KEY_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_NODE_VALUE: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_MAP_NODE_VALUE_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_ROOT: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_INTMAP_ROOT_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_VALUE_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_LEFT_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_STORAGE_TREE_MAP_ROOT: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_MAP_ROOT_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfStorage.SELECTOR_EVENT_CREATOR: return new UpdateOfStorage(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.EVENT_CREATOR_FIELD, StorageReferenceImpl.fromWithoutSelector(context));
		case UpdateOfString.SELECTOR_PUBLIC_KEY: return new UpdateOfString(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.EOA_PUBLIC_KEY_FIELD, context.readStringUnshared());
		case UpdateOfString.SELECTOR: return new UpdateOfString(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readStringUnshared());
		case UpdateToNullEager.SELECTOR: return new UpdateToNullEager(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context));
		case UpdateToNullLazy.SELECTOR: return new UpdateToNullLazy(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context));
		default: throw new IOException("Unexpected update selector: " + selector);
		}
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}