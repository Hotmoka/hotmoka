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
import io.hotmoka.node.api.updates.UpdateOfBoolean;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.json.UpdateJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * The implementation of an update of a field of type {@code boolean}.
 */
@Immutable
public final class UpdateOfBooleanImpl extends UpdateOfFieldImpl implements UpdateOfBoolean {
	final static byte SELECTOR_FALSE = 3;
	final static byte SELECTOR_TRUE = 4;

	/**
	 * The new value of the field.
	 */
	private final boolean value;

	/**
	 * Builds an update of a {@code boolean} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfBooleanImpl(StorageReference object, FieldSignature field, boolean value) {
		this(object, field, value, IllegalArgumentException::new);
	}

	/**
	 * Builds an update of a {@code boolean} field from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @param value the assigned value
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public UpdateOfBooleanImpl(UpdateJson json, boolean value) throws InconsistentJsonException {
		this(
			unmapObject(json),
			unmapField(json),
			value,
			InconsistentJsonException::new
		);
	}

	/**
	 * Unmarshals an update of a {@code boolean} field from the given context.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @throws IOException if the unmarshalling failed
	 */
	public UpdateOfBooleanImpl(UnmarshallingContext context, byte selector) throws IOException {
		this(
			StorageReferenceImpl.fromWithoutSelector(context),
			FieldSignatures.from(context),
			unmarshalValue(context, selector),
			IOException::new
		);
	}

	/**
	 * Builds an update of an {@code boolean} field.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> UpdateOfBooleanImpl(StorageReference object, FieldSignature field, boolean value, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(object, field, onIllegalArgs);
	
		this.value = value;
	}

	private static boolean unmarshalValue(UnmarshallingContext context, byte selector) {
		if (selector == SELECTOR_TRUE)
			return true;
		else if (selector == SELECTOR_FALSE)
			return false;
		else
			throw new IllegalArgumentException("Unexpected selector " + selector + " for a boolean field update");
	}

	@Override
	public BooleanValue getValue() {
		return StorageValues.booleanOf(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfBoolean uob && super.equals(other) && uob.getValue().getValue() == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Boolean.hashCode(value);
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Boolean.compare(value, ((UpdateOfBooleanImpl) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(value ? SELECTOR_TRUE : SELECTOR_FALSE);
		super.into(context);
	}
}