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

import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.internal.AbstractVerificationError;

/**
 * An error issued when a {@code synchronized} is used in Takamaka code.
 */
public class IllegalSynchronizationError extends AbstractVerificationError {

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point where the error occurs
	 * @param method the method where the error occurs
	 * @param line the program line where the error occurs
	 */
	public IllegalSynchronizationError(String where, MethodGen method, int line) {
		super(where, method, line, "synchronization is not allowed");
	}

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point where the error occurs
	 * @param message the message of the error
	 */
	public IllegalSynchronizationError(String where, String message) {
		super(where, message);
	}
}