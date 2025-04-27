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

package io.hotmoka.verification.errors;

import org.apache.bcel.classfile.Field;

import io.hotmoka.verification.internal.AbstractVerificationError;

/**
 * An error issued when a field has a name not allowed in Takamaka,
 * for instance because it starts with an instrumentation prefix.
 */
public class IllegalFieldNameError extends AbstractVerificationError {

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point where the error occurs
	 * @param field the field where the error occurs
	 */
	public IllegalFieldNameError(String where, Field field) {
		super(where, field, "field name \"" + field.getName() + "\" is not allowed");
	}

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point where the error occurs
	 * @param message the message of the error
	 */
	public IllegalFieldNameError(String where, String message) {
		super(where, message);
	}
}