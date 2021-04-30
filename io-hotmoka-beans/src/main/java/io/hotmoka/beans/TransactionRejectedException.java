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

package io.hotmoka.beans;

/**
 * An exception raised when a transaction cannot even be started.
 * Typically, this means that the payer of the transaction cannot be identified
 * or it has not enough money to pay for a failed transaction or that its signature
 * is invalid.
 */
@SuppressWarnings("serial")
public class TransactionRejectedException extends Exception {

	/**
	 * Builds an exception with the given message.
	 * 
	 * @param message the message
	 */
	public TransactionRejectedException(String message) {
		super(message);
	}

	/**
	 * Builds an exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public TransactionRejectedException(Throwable cause) {
		super(cause.getClass().getName() + messageOf(cause), cause);
	}

	private static String messageOf(Throwable cause) {
		return cause.getMessage() == null ? "" : (": " + cause.getMessage());
	}
}