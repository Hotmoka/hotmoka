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
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.updates.UpdateOfField;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Implementation of an update of a field of an object.
 */
@Immutable
public abstract class UpdateOfFieldImpl extends AbstractUpdate implements UpdateOfField {

	/**
	 * The field that is modified.
	 */
	protected final FieldSignature field;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 */
	protected UpdateOfFieldImpl(StorageReference object, FieldSignature field) {
		super(object);

		this.field = Objects.requireNonNull(field, "field cannot be null");
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