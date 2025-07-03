/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.helpers.api;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnexpectedCodeException;

/**
 * An object that helps with gas operations.
 */
@ThreadSafe
public interface GasHelper {

	/**
	 * Yields the gas price for a transaction.
	 * 
	 * @return the gas price
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka runtime is behaving in an unexpected way
	 * @throws CodeExecutionException if some transaction throws an exception
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	BigInteger getGasPrice() throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, UnexpectedCodeException, TimeoutException, InterruptedException;

	/**
	 * Yields a safe gas price for a transaction, that should be valid
	 * for a little time, also in case of small fluctuations in the gas price.
	 * This is simply the double of {@link #getGasPrice()}.
	 * 
	 * @return a safe gas price
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction throws an exception
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka runtime is behaving in an unexpected way
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	BigInteger getSafeGasPrice() throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, UnexpectedCodeException, TimeoutException, InterruptedException;
}