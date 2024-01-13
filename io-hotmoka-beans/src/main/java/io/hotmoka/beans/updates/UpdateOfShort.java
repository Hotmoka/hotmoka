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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * An update of a field states that a short field of a given storage object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the shape of storage objects.
 */
@Immutable
public final class UpdateOfShort extends UpdateOfField {
	final static byte SELECTOR = 15;

	/**
	 * The new value of the field.
	 */
	public final short value;

	/**
	 * Builds an update of an {@code short} field.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 * @param value the new value of the field
	 */
	public UpdateOfShort(StorageReference object, FieldSignature field, short value) {
		super(object, field);

		this.value = value;
	}

	@Override
	public StorageValue getValue() {
		return StorageValues.shortOf(value);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateOfShort uos && super.equals(other) && uos.value == value;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ value;
	}

	@Override
	public int compareTo(Update other) {
		int diff = super.compareTo(other);
		if (diff != 0)
			return diff;
		else
			return Short.compare(value, ((UpdateOfShort) other).value);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		context.writeShort(value);
	}
}