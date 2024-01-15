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

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.api.signatures.ConstructorSignature;

/**
 * The model of the signature of a constructor of a class.
 */
public final class ConstructorSignatureModel extends CodeSignatureModel {

	/**
	 * Builds the model from the signature of a constructor.
	 * 
	 * @param constructor the signature to copy
	 */
	public ConstructorSignatureModel(ConstructorSignature constructor) {
		super(constructor);
	}

	public ConstructorSignatureModel() {}

	/**
	 * Yields the constructor signature corresponding to this model.
	 * 
	 * @return the constructor signature
	 */
	public ConstructorSignature toBean() {
		return ConstructorSignatures.of(definingClass, getFormalsAsTypes());
	}
}