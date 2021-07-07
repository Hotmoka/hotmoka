/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.network.signatures;

import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.types.StorageType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The model of the signature of a method or constructor.
 */
public abstract class CodeSignatureModel extends SignatureModel {

	/**
	 * The formal arguments of the method or constructor.
	 */
	public List<String> formals;

	/**
	 * Builds the model of the signature of a method or constructor.
	 * 
	 * @param signature the original signature to copy
	 */
	protected CodeSignatureModel(CodeSignature signature) {
		super(signature.definingClass.name);

		this.formals = signature.formals().map(CodeSignatureModel::nameOf).collect(Collectors.toList());
	}

	public CodeSignatureModel() {}


	/**
	 * Yields the storage types of the formal arguments of this method or constructor.
	 * 
	 * @return the storage types
	 */
	protected final StorageType[] getFormalsAsTypes() {
		return this.formals.stream().map(SignatureModel::typeWithName).toArray(StorageType[]::new);
	}
}