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

package io.hotmoka.node.internal.gson;

import java.util.stream.Stream;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.types.StorageType;
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
		this.name = method.getMethodName();
		this.formals = method.getFormals().map(StorageType::getName).toArray(String[]::new);
		this.returnType = method instanceof NonVoidMethodSignature nvms ? nvms.getReturnType().getName() : null;
	}

	@Override
	public MethodSignature unmap() {
		if (returnType == null)
			return MethodSignatures.ofVoid(StorageTypes.classNamed(definingClass), name, Stream.of(formals).map(StorageTypes::named));
		else
			return MethodSignatures.ofNonVoid(StorageTypes.classNamed(definingClass), name, StorageTypes.named(returnType), Stream.of(formals).map(StorageTypes::named));
	}
}