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

import java.util.stream.Stream;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link ConstructorSignature}.
 */
public abstract class ConstructorSignatureJson implements JsonRepresentation<ConstructorSignature> {
	private final String definingClass;
	private final String[] formals;

	protected ConstructorSignatureJson(ConstructorSignature constructor) {
		this.definingClass = constructor.getDefiningClass().getName();
		this.formals = constructor.getFormals().map(StorageType::getName).toArray(String[]::new);
	}

	@Override
	public ConstructorSignature unmap() {
		return ConstructorSignatures.of(StorageTypes.classNamed(definingClass), Stream.of(formals).map(StorageTypes::named));
	}
}