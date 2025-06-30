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
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.StorageReference;

/**
 * Implementation of an update of a field of an object.
 */
@Immutable
abstract class UpdateOfFieldImpl extends AbstractUpdate implements UpdateOfField {

	/**
	 * The field that is modified.
	 */
	protected final FieldSignature field;

	/**
	 * Builds an update.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param onIllegalArgs the supplier of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	protected <E extends Exception> UpdateOfFieldImpl(StorageReference object, FieldSignature field, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		super(object, onIllegalArgs);

		this.field = Objects.requireNonNull(field, "field cannot be null", onIllegalArgs);
	}

	@Override
	public final FieldSignature getField() {
		return field;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfField uof && super.equals(other) && uof.getField().equals(field);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ field.hashCode();
	}

	@Override
	public String toString() {
		return "<" + getObject() + "|" + getField() + "|" + getValue() + ">";
	}

	@Override
	public final boolean sameProperty(Update other) {
		return other instanceof UpdateOfField uof && field.equals(uof.getField());
	}

	@Override
	public int compareTo(Update other) {
		if (other instanceof UpdateOfField uof) {
			int diff = field.compareTo(uof.getField());
			if (diff != 0)
				return diff;
		}

		return super.compareTo(other);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		super.into(context);
		field.into(context);
	}

	/**
	 * Marshals this object into the given context, without the field information.
	 * 
	 * @param context the context
	 * @throws IOException if marshalling fails
	 */
	protected final void intoWithoutField(MarshallingContext context) throws IOException {
		super.into(context);
	}
}