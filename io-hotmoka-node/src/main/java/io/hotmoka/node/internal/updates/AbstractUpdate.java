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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
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
import io.hotmoka.node.internal.json.UpdateJson;
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
	protected <E extends Exception> AbstractUpdate(StorageReference object, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
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

		case UpdateOfBigIntegerImpl.SELECTOR_BALANCE:
		case UpdateOfBigIntegerImpl.SELECTOR_GAS_PRICE:
		case UpdateOfBigIntegerImpl.SELECTOR_UBI_VALUE:
		case UpdateOfBigIntegerImpl.SELECTOR_BALANCE_TO_ZERO:
		case UpdateOfBigIntegerImpl.SELECTOR_NONCE_TO_ZERO:
		case UpdateOfBigIntegerImpl.SELECTOR_NONCE:
		case UpdateOfBigIntegerImpl.SELECTOR_CURRENT_SUPPLY:
		case UpdateOfBigIntegerImpl.SELECTOR_HEIGHT:
		case UpdateOfBigIntegerImpl.SELECTOR_NUMBER_OF_TRANSACTIONS:
		case UpdateOfBigIntegerImpl.SELECTOR: return new UpdateOfBigIntegerImpl(context, selector);

		case UpdateOfBooleanImpl.SELECTOR_FALSE:
		case UpdateOfBooleanImpl.SELECTOR_TRUE: return new UpdateOfBooleanImpl(context, selector);

		case UpdateOfByteImpl.SELECTOR: return new UpdateOfByteImpl(context);

		case UpdateOfCharImpl.SELECTOR: return new UpdateOfCharImpl(context);

		case UpdateOfDoubleImpl.SELECTOR: return new UpdateOfDoubleImpl(context);

		case UpdateOfFloatImpl.SELECTOR: return new UpdateOfFloatImpl(context);

		case UpdateOfIntImpl.SELECTOR:
		case UpdateOfIntImpl.SELECTOR_SMALL:
		case UpdateOfIntImpl.SELECTOR_VERY_SMALL:
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_MAP_NODE_SIZE:
		case UpdateOfIntImpl.SELECTOR_SNAPSHOTTABLE_STORAGE_TREE_MAP_NODE_SIZE:
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_SIZE:
		case UpdateOfIntImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_KEY: return new UpdateOfIntImpl(context, selector);

		case UpdateOfLongImpl.SELECTOR: return new UpdateOfLongImpl(context);

		case UpdateOfShortImpl.SELECTOR: return new UpdateOfShortImpl(context);

		case UpdateOfStorageImpl.SELECTOR:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_LEFT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_RIGHT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_KEY:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_NODE_VALUE:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_MAP_ROOT:
		case UpdateOfStorageImpl.SELECTOR_SNAPSHOTTABLE_STORAGE_TREE_MAP_NODE_LEFT:
		case UpdateOfStorageImpl.SELECTOR_SNAPSHOTTABLE_STORAGE_TREE_MAP_NODE_RIGHT:
		case UpdateOfStorageImpl.SELECTOR_SNAPSHOTTABLE_STORAGE_TREE_MAP_NODE_KEY:
		case UpdateOfStorageImpl.SELECTOR_SNAPSHOTTABLE_STORAGE_TREE_MAP_NODE_VALUE:
		case UpdateOfStorageImpl.SELECTOR_SNAPSHOTTABLE_STORAGE_TREE_MAP_ROOT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_ROOT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_VALUE:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_LEFT:
		case UpdateOfStorageImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE_RIGHT:
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

		var value = Objects.requireNonNull(json.getValue(), "A field update must have non-null value", InconsistentJsonException::new).unmap();

		if (value instanceof BigIntegerValue biv)
			return new UpdateOfBigIntegerImpl(json, biv.getValue());
		else if (value instanceof BooleanValue bv)
			return new UpdateOfBooleanImpl(json, bv.getValue());
		else if (value instanceof ByteValue bv)
			return new UpdateOfByteImpl(json, bv.getValue());
		else if (value instanceof CharValue cv)
			return new UpdateOfCharImpl(json, cv.getValue());
		else if (value instanceof DoubleValue dv)
			return new UpdateOfDoubleImpl(json, dv.getValue());
		else if (value instanceof FloatValue fv)
			return new UpdateOfFloatImpl(json, fv.getValue());
		else if (value instanceof IntValue iv)
			return new UpdateOfIntImpl(json, iv.getValue());
		else if (value instanceof LongValue lv)
			return new UpdateOfLongImpl(json, lv.getValue());
		else if (value instanceof ShortValue sv)
			return new UpdateOfShortImpl(json, sv.getValue());
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
	protected final MarshallingContext createMarshallingContext(OutputStream os) {
		return NodeMarshallingContexts.of(os);
	}
}