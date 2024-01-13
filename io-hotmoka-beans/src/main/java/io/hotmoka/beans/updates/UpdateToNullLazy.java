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
 * An update that states that the field of a given storage object has been
 * modified to {@code null}. The field is declared with a lazy type.
 * Updates are stored in blockchain and describe the shape of storage objects.
 */
@Immutable
public final class UpdateToNullLazy extends UpdateOfField {
	final static byte SELECTOR = 19;

	/**
	 * Builds an update of a {@link java.math.BigInteger} field of lazy type.
	 * 
	 * @param object the storage reference of the object whose field is modified
	 * @param field the field that is modified
	 */
	public UpdateToNullLazy(StorageReference object, FieldSignature field) {
		super(object, field);
	}

	@Override
	public StorageValue getValue() {
		return StorageValues.NULL;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UpdateToNullLazy && super.equals(other);
	}

	@Override
	public boolean isEager() {
		return false;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
	}
}