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

import java.util.stream.Stream;

import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.signatures.ConstructorSignatureImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link ConstructorSignature}.
 */
public abstract class ConstructorSignatureJson implements JsonRepresentation<ConstructorSignature> {
	private final String definingClass;
	private final String[] formals;

	protected ConstructorSignatureJson(ConstructorSignature constructor) {
		this.definingClass = constructor.getDefiningClass().getName();

		StorageType[] formals = constructor.getFormals().toArray(StorageType[]::new);
		if (formals.length == 0)
			// optimization, to compact the JSON
			this.formals = null;
		else {
			this.formals = new String[formals.length];
			for (int pos = 0; pos < formals.length; pos++)
				this.formals[pos] = formals[pos].getName();
		}
	}

	/**
	 * Yields the name of the class defining the Constructor.
	 * 
	 * @return the name of the class
	 */
	public String getDefiningClass() {
		return definingClass;
	}

	/**
	 * Yields the string description of the types of the formal arguments of the constructor.
	 * 
	 * @return the string description of the types of the formal arguments of the constructor
	 */
	public Stream<String> getFormals() {
		return formals == null ? Stream.empty() : Stream.of(formals);
	}

	@Override
	public ConstructorSignature unmap() throws InconsistentJsonException {
		return new ConstructorSignatureImpl(this);
	}
}