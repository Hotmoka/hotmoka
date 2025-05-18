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

import io.hotmoka.helpers.api.SendCoinsHelper;
import io.hotmoka.helpers.internal.SendCoinsHelperImpl;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * Providers of objects that help with sending coins to accounts.
 */
public abstract class SendCoinsHelpers {

	private SendCoinsHelpers() {}

	/**
	 * Yields an object that helps with sending coins to accounts.
	 * 
	 * @param node the node whose accounts are considered
	 * @return the send coin helper
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 * @throws CodeExecutionException if some transaction threw an exception
	 * @throws TransactionException if some transaction failed
	 * @throws TransactionRejectedException if some transaction was rejected
	 */
	public static SendCoinsHelper of(Node node) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException {
		return new SendCoinsHelperImpl(node);
	}
}
