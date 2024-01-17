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

package io.hotmoka.beans.internal.gson;

import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link FieldSignature}.
 */
public abstract class FieldSignatureJson implements JsonRepresentation<FieldSignature> {
	private final String definingClass;
	private final String name;
	private final String type;

	protected FieldSignatureJson(FieldSignature field) {
		this.definingClass = field.getDefiningClass().getName();
		this.name = field.getName();
		this.type = field.getType().getName();
	}

	@Override
	public FieldSignature unmap() {
		return FieldSignatures.of(StorageTypes.classNamed(definingClass), name, StorageTypes.named(type));
	}
}