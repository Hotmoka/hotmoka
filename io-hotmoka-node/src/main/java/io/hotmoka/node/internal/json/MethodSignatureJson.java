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

import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.signatures.AbstractMethodSignature;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link MethodSignature}.
 */
public abstract class MethodSignatureJson implements JsonRepresentation<MethodSignature> {
	private final String definingClass;
	private final String name;
	private final String[] formals;
	private final String returnType;

	protected MethodSignatureJson(MethodSignature method) {
		this.definingClass = method.getDefiningClass().getName();
		this.name = method.getName();

		var formals = method.getFormals().toArray(StorageType[]::new);
		if (formals.length == 0)
			// optimization, to compact the JSON
			this.formals = null;
		else {
			this.formals = new String[formals.length];
			for (int pos = 0; pos < formals.length; pos++)
				this.formals[pos] = formals[pos].getName();
		}

		this.returnType = method instanceof NonVoidMethodSignature nvms ? nvms.getReturnType().getName() : null;
	}

	public String getDefiningClass() {
		return definingClass;
	}

	public String getName() {
		return name;
	}

	public Stream<String> getFormals() {
		return formals == null ? Stream.empty() : Stream.of(formals);
	}

	/**
	 * Yields the return type of the method described by this JSON object.
	 * This is empty if the method returns {@code void}.
	 * 
	 * @return the return type of the method represented by this JSON object, if any
	 */
	public Optional<String> getReturnType() {
		return Optional.ofNullable(returnType);
	}

	@Override
	public MethodSignature unmap() throws InconsistentJsonException {
		return AbstractMethodSignature.from(this);
	}
}