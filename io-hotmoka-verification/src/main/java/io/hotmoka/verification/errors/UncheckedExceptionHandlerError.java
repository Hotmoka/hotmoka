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
 * An error issued when an exception handler for an unchecked exception is used.
 */
public class UncheckedExceptionHandlerError extends AbstractVerificationError {

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point where the error occurs
	 * @param method the method where the error occurs
	 * @param line the program line where the error occurs
	 * @param exceptionName the name of the caught exception
	 */
	public UncheckedExceptionHandlerError(String where, MethodGen method, int line, String exceptionName) {
		super(where, method, line, "exception handler for " + exceptionName + " might check unchecked exceptions");
	}

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point where the error occurs
	 * @param message the message of the error
	 */
	public UncheckedExceptionHandlerError(String where, String message) {
		super(where, message);
	}
}