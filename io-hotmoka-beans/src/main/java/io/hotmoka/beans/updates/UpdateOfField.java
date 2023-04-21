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
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.BeanMarshallingContext;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * An update of a field states that the field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public abstract class UpdateOfField extends Update {

	/**
	 * The field that is modified.
	 */
	public final FieldSignature field;

	/**
	 * Builds an update.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 */
	protected UpdateOfField(StorageReference object, FieldSignature field) {
		super(object);

		this.field = field;
	}

	/**
	 * Yields the field whose value is updated.
	 *
	 * @return the field
	 */
	public final FieldSignature getField() {
		return field;
	}

	/**
	 * Yields the value set into the updated field.
	 * 
	 * @return the value
	 */
	public abstract StorageValue getValue();

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfField && super.equals(other) && ((UpdateOfField) other).field.equals(field);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ field.hashCode();
	}

	@Override
	public String toString() {
		return "<" + object + "|" + getField() + "|" + getValue() + ">";
	}

	@Override
	public final boolean sameProperty(Update other) {
		return other instanceof UpdateOfField && getField().equals(((UpdateOfField) other).getField());
	}

	@Override
	public int compareTo(Update other) {
		if (other instanceof UpdateOfField) {
			int diff = field.compareTo(((UpdateOfField) other).field);
			if (diff != 0)
				return diff;
		}

		return super.compareTo(other);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(field.size(gasCostModel));
	}

	@Override
	public void into(BeanMarshallingContext context) throws IOException {
		super.into(context);
		field.into(context);
	}

	protected final void intoWithoutField(BeanMarshallingContext context) throws IOException {
		super.into(context);
	}
}