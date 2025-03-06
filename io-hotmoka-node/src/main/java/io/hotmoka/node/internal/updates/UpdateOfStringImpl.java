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
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfString;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;

/**
 * The implementation of an update of a field of type string.
 */
@Immutable
public final class UpdateOfStringImpl extends UpdateOfFieldImpl implements UpdateOfString {
	final static byte SELECTOR = 17;
	final static byte SELECTOR_PUBLIC_KEY = 32;

	/**
	 * The new value of the field.
	 */
	private final String value;

	/**
	 * Builds an update of a string field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfStringImpl(StorageReference object, FieldSignature field, String value) {
		super(object, field);

		this.value = Objects.requireNonNull(value, "value cannot be null");
	}

	@Override
	public StringValue getValue() {
		return StorageValues.stringOf(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfString uos && super.equals(other) && uos.getValue().getValue().equals(value);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value.hashCode();
	}

	@Override
	public String toString() {
		// we add the double quotes around the string literal
		return "<" + getObject() + "|" + getField() + "|\"" + getValue() + "\">";
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((UpdateOfStringImpl) other).value);
	}

	@Override
	public boolean isEager() {
		// a lazy String could also be stored into a lazy Object or Serializable or Comparable or CharSequence field
		return StorageTypes.STRING.equals(field.getType());
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (FieldSignatures.EOA_PUBLIC_KEY_FIELD.equals(field)) {
			context.writeByte(SELECTOR_PUBLIC_KEY);
			super.intoWithoutField(context);
		}
		else {
			context.writeByte(SELECTOR);
			super.into(context);
		}

		context.writeStringUnshared(value);
	}
}