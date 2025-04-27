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

import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.internal.signatures.FieldSignatureImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
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

	public String getDefiningClass() {
		return definingClass;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	@Override
	public FieldSignature unmap() throws InconsistentJsonException {
		return new FieldSignatureImpl(this);
	}
}