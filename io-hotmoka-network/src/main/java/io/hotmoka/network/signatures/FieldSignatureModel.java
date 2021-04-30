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

package io.hotmoka.network.signatures;

import io.hotmoka.beans.signatures.FieldSignature;

/**
 * The model of the signature of a field of a class.
 */
public final class FieldSignatureModel extends SignatureModel {

	/**
	 * The name of the field.
	 */
	public String name;

	/**
	 * The type of the field.
	 */
	public String type;

	/**
	 * Builds the model of the signature of a field.
	 * 
	 * @param field the signature of the field
	 */
	public FieldSignatureModel(FieldSignature field) {
		super(field.definingClass.name);

		this.name = field.name;
		this.type = nameOf(field.type);
	}

	public FieldSignatureModel() {}

	/**
	 * Yields the signature having this model.
	 * 
	 * @return the signature
	 */
	public FieldSignature toBean() {
		return new FieldSignature(definingClass, name, typeWithName(type));
	}
}