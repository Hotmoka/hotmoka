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
 * An error issued when {@code @@Payable} is not applied to a method or
 * constructor of a contract or interface,
 */
public class PayableNotInContractError extends AbstractVerificationError {

	/**
	 * Builds the error.
	 *
	 * @param where the description of the program point where the error occurs
	 * @param method the method where the error occurs
	 */
	public PayableNotInContractError(String where, MethodGen method) {
		super(where, method, -1, "@Payable can only be applied to constructors or instance methods of a contract class or of an interface");
	}

	/**
	 * Builds the error.
	 * 
	 * @param where the description of the program point where the error occurs
	 * @param message the message of the error
	 */
	public PayableNotInContractError(String where, String message) {
		super(where, message);
	}
}