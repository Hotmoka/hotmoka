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

package io.hotmoka.node.api;

/**
 * A wrapper of an exception, raised during a transaction, that occurred during
 * the execution of a Takamaka constructor or method that was not allowed to throw it.
 */
@SuppressWarnings("serial")
public class TransactionException extends Exception {

	/**
	 * Builds an exception, raised during a transaction, that occurred during
	 * the execution of a Takamaka constructor or method that was not allowed to throw it.
	 * 
	 * @param classNameOfCause the name of the class of the cause of the exception
	 * @param messageOfCause the message of the cause of the exception. This might be {@code null}
	 * @param where a description of the program point of the exception. This might be {@code null}
	 */
	public TransactionException(String classNameOfCause, String messageOfCause, String where) {
		super(classNameOfCause
			+ (messageOfCause.isEmpty() ? "" : (": " + messageOfCause))
			+ (where.isEmpty() ? "" : "@" + where));
	}

	/**
	 * Builds an exception, raised during a transaction, that occurred during
	 * the execution of a Takamaka constructor or method that was not allowed to throw it.
	 * 
	 * @param message the message of the exception
	 */
	public TransactionException(String message) {
		super(message);
	}
}