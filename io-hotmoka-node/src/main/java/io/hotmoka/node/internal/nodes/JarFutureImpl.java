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

package io.hotmoka.node.internal.nodes;

import java.util.concurrent.TimeoutException;

import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * Implementation of the future of a transaction that installs a jar in a node.
 * It caches the result for repeated use.
 */
public class JarFutureImpl implements JarFuture {
	private final TransactionReference reference;
	private final Node node;

	/**
	 * The cached result.
	 */
	private volatile TransactionReference cachedGet;

	/**
	 * Creates a future for the installation of a jar in a node, with a transaction request
	 * that has the given reference.
	 * 
	 * @param reference the reference to the request of the transaction
	 * @param node the node where the jar is installed
	 */
	public JarFutureImpl(TransactionReference reference, Node node) {
		this.reference = reference;
		this.node = node;
	}

	@Override
	public TransactionReference getReferenceOfRequest() {
		return reference;
	}

	@Override
	public TransactionReference get() throws TransactionRejectedException, TransactionException, NodeException, TimeoutException, InterruptedException {
		if (cachedGet != null)
			return cachedGet;

		var response = node.getPolledResponse(reference);
		if (response instanceof JarStoreTransactionResponse jstr)
			return cachedGet = getOutcome(jstr);
		else
			throw new NodeException("Wrong type " + response.getClass().getName() + " for the response of the jar store request " + reference);
	}

	private TransactionReference getOutcome(JarStoreTransactionResponse response) throws TransactionException {
		if (response instanceof JarStoreTransactionFailedResponse jstfr)
			throw new TransactionException(jstfr.getClassNameOfCause(), jstfr.getMessageOfCause(), "");
		else
			return reference;
	}
}