/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans.api.responses;

import io.hotmoka.annotations.Immutable;

/**
 * A response for a successful transaction that calls a method in the store of the node.
 * The method is annotated as {@code io.takamaka.code.lang.ThrowsExceptions}.
 * It has been called without problems but it threw an exception.
 */
@Immutable
public interface MethodCallTransactionExceptionResponse extends MethodCallTransactionResponse, TransactionResponseWithEvents {

	/**
	 * Yields the fully-qualified class name of the cause exception.
	 * 
	 * @return the fully-qualified class name of the cause exception
	 */
	String getClassNameOfCause();

	/**
	 * Yields the message of the cause exception.
	 * 
	 * @return the message of the cause exception
	 */
	String getMessageOfCause();

	/**
	 * Yields the program point where the cause exception occurred.
	 * 
	 * @return the program point
	 */
	String getWhere();
}