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

import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.api.SignatureHelper;
import io.hotmoka.helpers.internal.SignatureHelperImpl;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * Providers of helpers to determine the signature algorithm to use for an externally owned account.
 */
public class SignatureHelpers {
	private SignatureHelpers() {}

	/**
	 * Yields a signature helper for the given node.
	 * 
	 * @param node the node
	 * @return the signature helper
	 * @throws TransactionRejectedException if some transaction that installs the jars is rejected
	 * @throws TransactionException if some transaction that installs the jars fails
	 * @throws CodeExecutionException if some transaction that installs the jars throws an exception
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 * @throws NoSuchElementException if the node is not properly initialized
	 */
	public static SignatureHelper of(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return new SignatureHelperImpl(node);
	}
}