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

import java.math.BigInteger;

/**
 * The response of a failed transaction. This means that the transaction
 * could not be executed until its end. Instance gas provided to the
 * transaction has been consumed, as a form of penalty.
 */
public interface FailedTransactionResponse extends TransactionResponse {

	/**
	 * Yields the amount of gas that the transaction consumed for penalty, since it failed.
	 * 
	 * @return the amount of gas
	 */
	BigInteger getGasConsumedForPenalty();

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
}