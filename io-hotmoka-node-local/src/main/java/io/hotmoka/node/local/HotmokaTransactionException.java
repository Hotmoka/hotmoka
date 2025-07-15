/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.node.local;

import java.util.Objects;

/**
 * An exception occurred during the execution of a Hotmoka transaction.
 * This is not a bug in the code of the node, but a wrong specification of the request
 * of the transaction (that, for instance, makes it impossible to find the target of
 * a method call), or a limit of the execution (for instance, the gas is over),
 * or a bug in the storage code (for instance, the return value of a method
 * cannot be serialized into a storage value). These transactions are not
 * meant to be caught but just to lead to a failed transaction response.
 */
@SuppressWarnings("serial")
public abstract class HotmokaTransactionException extends RuntimeException {

	/**
	 * Creates the exception with a message.
	 * 
	 * @param message the message of the exception
	 */
	protected HotmokaTransactionException(String message) {
		super(Objects.requireNonNull(message));
	}

	/**
	 * Creates the exception with message and cause.
	 * 
	 * @param message the message of the exception
	 * @param cause the cause of the exception
	 */
	protected HotmokaTransactionException(String message, Throwable cause) {
		super(Objects.requireNonNull(message), Objects.requireNonNull(cause));
	}
}