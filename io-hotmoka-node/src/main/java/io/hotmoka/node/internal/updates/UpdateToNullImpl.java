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
import io.hotmoka.beans.api.updates.UpdateToNull;
import io.hotmoka.beans.api.values.NullValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.StorageValues;

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
		super(object, field);

		this.eager = eager;
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