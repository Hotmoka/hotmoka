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

package io.hotmoka.beans.api.requests;

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;

/**
 * A request for a transaction that can only be run after the node has been initialized.
 *
 * @param <R> the type of the corresponding response
 */
@Immutable
public interface NonInitialTransactionRequest<R extends NonInitialTransactionResponse> extends TransactionRequest<R> {

	/**
	 * Yields the externally owned caller contract that pays for the transaction.
	 * 
	 * @return the caller
	 */
	StorageReference getCaller();

	/**
	 * Yields the amount of gas provided to the transaction.
	 * 
	 * @return the amount of gas
	 */
	BigInteger getGasLimit();

	/**
	 * Yields the coins paid for each unit of gas consumed by the transaction.
	 *
	 * @return the coins paid for each unit of gas consumed by the transaction
	 */
	BigInteger getGasPrice();

	/**
	 * Yields the class path that specifies where the {@code caller} should be interpreted.
	 * 
	 * @return the class path
	 */
	TransactionReference getClasspath();

	/**
	 * Yields the nonce used for transaction ordering and to forbid transaction replay on the same chain.
	 * It is relative to the caller.
	 */
	BigInteger getNonce();
}