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

import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;

/**
 * The model of the signature of a method of a class.
 */
public class MethodSignatureModel extends CodeSignatureModel {

	/**
	 * The name of the method.
	 */
	public String methodName;

	/**
	 * The return type of the method, if any.
	 */
	public String returnType;

	/**
	 * Builds the model of the signature of a method.
	 * 
	 * @param method the method original signature to copy
	 */
	public MethodSignatureModel(MethodSignature method) {
		super(method);

		this.methodName = method.methodName;
		if (method instanceof NonVoidMethodSignature)
			returnType = nameOf(((NonVoidMethodSignature) method).returnType);
		else
			returnType = null;
	}

	public MethodSignatureModel() {}

	/**
	 * Yields the method signature corresponding to this model.
	 * 
	 * @return the method signature
	 */
	public MethodSignature toBean() {
		if (returnType == null)
			return new VoidMethodSignature(definingClass, methodName, getFormalsAsTypes());
		else
			return new NonVoidMethodSignature(definingClass, methodName, typeWithName(returnType), getFormalsAsTypes());
	}
}