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
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.UpdateToNull;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.json.UpdateJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of an update of a field to {@code null}.
 */
@Immutable
public final class UpdateToNullImpl extends UpdateOfFieldImpl implements UpdateToNull {
	final static byte SELECTOR_EAGER = 18;
	final static byte SELECTOR_LAZY = 19;

	/**
	 * True if and only if the update is eager.
	 */
	private final boolean eager;

	/**
	 * Builds an update of a field to {@code null}.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param eager true if and only if the update is eager
	 */
	public UpdateToNullImpl(StorageReference object, FieldSignature field, boolean eager) {
		this(object, field, eager, IllegalArgumentException::new);
	}

	/**
	 * Builds an update of a field to {@code null} from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public UpdateToNullImpl(UpdateJson json) throws InconsistentJsonException {
		this(
			unmapObject(json),
			unmapField(json),
			json.isEager(),
			InconsistentJsonException::new
		);
	}

	/**
	 * Unmarshals an update of a field to {@code null} from the given context.
	 * The selector has been already unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @throws IOException if the unmarshalling failed
	 */
	public UpdateToNullImpl(UnmarshallingContext context, byte selector) throws IOException {
		this(
			StorageReferenceImpl.fromWithoutSelector(context),
			FieldSignatures.from(context),
			unmarshalEager(selector),
			IOException::new
		);
	}

	/**
	 * Builds an update of a field to {@code null}.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param eager true if and only if the update is eager
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> UpdateToNullImpl(StorageReference object, FieldSignature field, boolean eager, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(object, field, onIllegalArgs);
	
		this.eager = eager;
	}

	private static boolean unmarshalEager(byte selector) {
		if (selector == SELECTOR_EAGER)
			return true;
		else if (selector == SELECTOR_LAZY)
			return false;
		else
			throw new IllegalArgumentException("Unknown selector " + selector + " for an update to null");
	}

	@Override
	public NullValue getValue() {
		return StorageValues.NULL;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateToNull uon && super.equals(other) && uon.isEager() == eager;
	}

	@Override
	public boolean isEager() {
		return eager;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(eager ? SELECTOR_EAGER : SELECTOR_LAZY);
		super.into(context);
	}
}