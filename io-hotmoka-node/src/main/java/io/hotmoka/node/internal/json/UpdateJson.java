/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.internal.json;

import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.updates.UpdateToNull;
import io.hotmoka.node.internal.updates.AbstractUpdate;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of an {@link Update}.
 */
public abstract class UpdateJson implements JsonRepresentation<Update> {

	/**
	 * The updated object: this always exists.
	 */
	private final StorageValues.Json object;

	/**
	 * The updated field: this exists only for updates of fields.
	 */
	private final FieldSignatures.Json field;

	/**
	 * The value written into {@link #field}: this exists only for updates of fields.
	 */
	private final StorageValues.Json value;

	/**
	 * The class of the {@link #object}: this exists only for a class tag.
	 */
	private final String clazz;

	/**
	 * The reference to the transaction that installed the class of {@link #object}:
	 * this exists only for a class tag.
	 */
	private final TransactionReferences.Json jar;

	/**
	 * True if and only if the update is eager: this exists only for updates to {@code null}.
	 */
	private final Boolean eager;

	protected UpdateJson(Update update) {
		this.object = new StorageValues.Json(update.getObject());

		if (update instanceof ClassTag ct) {
			this.clazz = ct.getClazz().getName();
			this.jar = new TransactionReferences.Json(ct.getJar());
			this.field = null;
			this.value = null;
			this.eager = null;
		}
		else if (update instanceof UpdateOfField uof) {
			this.field = new FieldSignatures.Json(uof.getField());
			this.value = new StorageValues.Json(uof.getValue());

			if (update instanceof UpdateToNull utn)
				this.eager = utn.isEager();
			else
				this.eager = null;

			this.clazz = null;
			this.jar = null;
		}
		else
			throw new IllegalArgumentException("Unexpected update of class " + update.getClass().getName());
	}

	public StorageValues.Json getObject() {
		return object;
	}

	public FieldSignatures.Json getField() {
		return field;
	}

	public StorageValues.Json getValue() {
		return value;
	}

	public String getClazz() {
		return clazz;
	}

	public TransactionReferences.Json getJar() {
		return jar;
	}

	public boolean isEager() {
		return Boolean.TRUE.equals(eager);
	}

	@Override
	public Update unmap() throws InconsistentJsonException {
		return AbstractUpdate.from(this);
	}
}