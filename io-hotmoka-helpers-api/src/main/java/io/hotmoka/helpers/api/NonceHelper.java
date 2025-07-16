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
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An object that helps with nonce operations.
 */
@ThreadSafe
public interface NonceHelper {

	/**
	 * Yields the nonce of an account.
	 * 
	 * @param account the account
	 * @return the nonce of {@code account}
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction throws an exception
	 * @throws UnknownReferenceException if {@code account} cannot be found in the node
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka support code in the node is unexpected
	 * @throws InterruptedException if the current thread gets interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 */
	BigInteger getNonceOf(StorageReference account) throws TransactionRejectedException, TransactionException, CodeExecutionException, UnknownReferenceException, ClosedNodeException, UnexpectedCodeException, InterruptedException, TimeoutException;
}