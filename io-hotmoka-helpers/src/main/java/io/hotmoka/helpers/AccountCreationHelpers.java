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

package io.hotmoka.helpers;

import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.api.AccountCreationHelper;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.helpers.internal.AccountCreationHelperImpl;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;

/**
 * Providers of objects that help with the creation of new accounts.
 */
public abstract class AccountCreationHelpers {
	private AccountCreationHelpers() {}

	/**
	 * Yields an object that helps with the creation of new accounts.
	 * 
	 * @param node the node whose accounts are considered
	 * @return the helper object
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws ClosedNodeException if the node is already closed
	 * @throws CodeExecutionException if some transaction threw an exception
	 * @throws TransactionException if some transaction failed
	 * @throws TransactionRejectedException if some transaction has been rejected
	 * @throws UninitializedNodeException if the node is not initialized yet
	 * @throws UnexpectedCodeException if the Takamaka runtime in the node is behaving in an unexpected way
	 */
	public static AccountCreationHelper of(Node node) throws ClosedNodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException, UninitializedNodeException, UnexpectedCodeException {
		return new AccountCreationHelperImpl(node);
	}
}
