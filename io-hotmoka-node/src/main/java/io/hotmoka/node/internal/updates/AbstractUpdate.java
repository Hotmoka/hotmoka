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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
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
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.internal.gson.UpdateJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

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
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param object the storage reference of the object whose field is modified
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	protected <E extends Exception> AbstractUpdate(StorageReference object, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		this.object = Objects.requireNonNull(object, "object cannot be null", onIllegalArgs);
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
		case ClassTagImpl.SELECTOR: return new ClassTagImpl(context);
		case UpdateOfBigIntegerImpl.SELECTOR_BALANCE: return new UpdateOfBigIntegerImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.BALANCE_FIELD, context.readBigInteger(), IOException::new);
		case UpdateOfBigIntegerImpl.SELECTOR_GAS_PRICE: return new UpdateOfBigIntegerImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.GENERIC_GAS_STATION_GAS_PRICE_FIELD, context.readBigInteger(), IOException::new);
		case UpdateOfBigIntegerImpl.SELECTOR_UBI_VALUE: return new UpdateOfBigIntegerImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.UNSIGNED_BIG_INTEGER_VALUE_FIELD, context.readBigInteger(), IOException::new);
		case UpdateOfBigIntegerImpl.SELECTOR_BALANCE_TO_ZERO: return new UpdateOfBigIntegerImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.BALANCE_FIELD, BigInteger.ZERO, IOException::new);
		case UpdateOfBigIntegerImpl.SELECTOR_NONCE_TO_ZERO: return new UpdateOfBigIntegerImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.EOA_NONCE_FIELD, BigInteger.ZERO, IOException::new);
		case UpdateOfBigIntegerImpl.SELECTOR_NONCE: return new UpdateOfBigIntegerImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.EOA_NONCE_FIELD, context.readBigInteger(), IOException::new);
		case UpdateOfBigIntegerImpl.SELECTOR: return new UpdateOfBigIntegerImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readBigInteger(), IOException::new);
		case UpdateOfBooleanImpl.SELECTOR_FALSE: return new UpdateOfBooleanImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), false, IOException::new);
		case UpdateOfBooleanImpl.SELECTOR_TRUE: return new UpdateOfBooleanImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), true, IOException::new);
		case UpdateOfByteImpl.SELECTOR: return new UpdateOfByteImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readByte(), IOException::new);
		case UpdateOfCharImpl.SELECTOR: return new UpdateOfCharImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readChar(), IOException::new);
		case UpdateOfDoubleImpl.SELECTOR: return new UpdateOfDoubleImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readDouble(), IOException::new);
		case UpdateOfFloatImpl.SELECTOR: return new UpdateOfFloatImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readFloat(), IOException::new);

		case UpdateOfIntImpl.SELECTOR: return new UpdateOfIntImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readInt(), IOException::new);
		case UpdateOfIntImpl.SELECTOR_SMALL: return new UpdateOfIntImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readShort(), IOException::new);
		case UpdateOfIntImpl.SELECTOR_VERY_SMALL: return new UpdateOfIntImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readByte(), IOException::new);
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_MAP_NODE_SIZE: return new UpdateOfIntImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_MAP_NODE_SIZE_FIELD, context.readCompactInt(), IOException::new);
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE: return new UpdateOfIntImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD, context.readCompactInt(), IOException::new);
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY: return new UpdateOfIntImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.STORAGE_TREE_INTMAP_NODE_KEY_FIELD, context.readCompactInt(), IOException::new);

		case UpdateOfLongImpl.SELECTOR: return new UpdateOfLongImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readLong(), IOException::new);

		case UpdateOfShortImpl.SELECTOR: return new UpdateOfShortImpl(StorageReferenceImpl.fromWithoutSelector(context), FieldSignatures.from(context), context.readShort(), IOException::new);

		case UpdateOfStorageImpl.SELECTOR:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_LEFT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_KEY:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_VALUE:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_ROOT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_ROOT:
		case UpdateOfStorageImpl.SELECTOR_EVENT_CREATOR: return new UpdateOfStorageImpl(context, selector);

		case UpdateOfStringImpl.SELECTOR_PUBLIC_KEY:
		case UpdateOfStringImpl.SELECTOR: return new UpdateOfStringImpl(context, selector);

		case UpdateToNullImpl.SELECTOR_EAGER:
		case UpdateToNullImpl.SELECTOR_LAZY: return new UpdateToNullImpl(context, selector);

		default: throw new IOException("Unexpected update selector: " + selector);
		}
	}

	/**
	 * Creates an update corresponding to the given JSON description.
	 * 
	 * @param json the JSON description
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public static Update from(UpdateJson json) throws InconsistentJsonException {
		if (json.getClazz() != null)
			return new ClassTagImpl(json);

		var object = unmapObject(json);
		var field = unmapField(json);
		var value = Objects.requireNonNull(json.getValue(), "A field update must have non-null value", InconsistentJsonException::new).unmap();

		if (value instanceof BigIntegerValue biv)
			return new UpdateOfBigIntegerImpl(object, field, biv.getValue(), InconsistentJsonException::new);
		else if (value instanceof BooleanValue bv)
			return new UpdateOfBooleanImpl(object, field, bv.getValue(), InconsistentJsonException::new);
		else if (value instanceof ByteValue bv)
			return new UpdateOfByteImpl(object, field, bv.getValue(), InconsistentJsonException::new);
		else if (value instanceof CharValue cv)
			return new UpdateOfCharImpl(object, field, cv.getValue(), InconsistentJsonException::new);
		else if (value instanceof DoubleValue dv)
			return new UpdateOfDoubleImpl(object, field, dv.getValue(), InconsistentJsonException::new);
		else if (value instanceof FloatValue fv)
			return new UpdateOfFloatImpl(object, field, fv.getValue(), InconsistentJsonException::new);
		else if (value instanceof IntValue iv)
			return new UpdateOfIntImpl(object, field, iv.getValue(), InconsistentJsonException::new);
		else if (value instanceof LongValue lv)
			return new UpdateOfLongImpl(object, field, lv.getValue(), InconsistentJsonException::new);
		else if (value instanceof ShortValue sv)
			return new UpdateOfShortImpl(object, field, sv.getValue(), InconsistentJsonException::new);
		else if (value instanceof StorageReference sr)
			return new UpdateOfStorageImpl(json, sr);
		else if (value instanceof StringValue sv)
			return new UpdateOfStringImpl(json, sv.getValue());
		else if (value instanceof NullValue)
			return new UpdateToNullImpl(json);
		else
			throw new InconsistentJsonException("Illegal update JSON");
	}

	protected static FieldSignature unmapField(UpdateJson json) throws InconsistentJsonException {
		return Objects.requireNonNull(json.getField(), "A field update must have non-null field", InconsistentJsonException::new).unmap();
	}

	protected static StorageReference unmapObject(UpdateJson json) throws InconsistentJsonException {
		if (Objects.requireNonNull(json.getObject(), "object cannot be null", InconsistentJsonException::new).unmap() instanceof StorageReference object)
			return object;
		else
			throw new InconsistentJsonException("A storage reference was expected as object");
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

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return NodeMarshallingContexts.of(os);
	}
}