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

import io.hotmoka.beans.api.types.StorageType;

/**
 * The model of the signature of a field, method or constructor.
 */
public abstract class SignatureModel {

	/**
	 * The name of the class defining the field, method or constructor.
	 */
	public String definingClass;

	/**
	 * Builds the model of the signature of a field, method or constructor.
	 * 
	 * @param definingClass the name of the class defining the field, method or constructor
	 */
	protected SignatureModel(String definingClass) {
		this.definingClass = definingClass;
	}

	protected SignatureModel() {}

	/**
	 * Yields a string representation of the given type.
	 * 
	 * @param type the type
	 * @return the string
	 */
	protected static String nameOf(StorageType type) {
    	if (type == null)
    		throw new RuntimeException("Unexpected null type");
    	else
    		return type.toString();
    }
}