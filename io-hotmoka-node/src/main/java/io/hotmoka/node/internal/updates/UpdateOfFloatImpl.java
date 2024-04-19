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
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.updates.UpdateOfFloat;
import io.hotmoka.beans.api.values.FloatValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.StorageValues;

/**
 * The implementation of an update of a field of type {@code float}.
 */
@Immutable
public final class UpdateOfFloatImpl extends UpdateOfFieldImpl implements UpdateOfFloat {
	final static byte SELECTOR = 10;

	/**
	 * The new value of the field.
	 */
	private final float value;

	/**
	 * Builds an update of an {@code float} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfFloatImpl(StorageReference object, FieldSignature field, float value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public FloatValue getValue() {
		return StorageValues.floatOf(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfFloat uof && super.equals(other) && uof.getValue().getValue() == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Float.hashCode(value);
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Float.compare(value, ((UpdateOfFloatImpl) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		context.writeFloat(value);
	}
}