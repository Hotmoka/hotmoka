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

package io.hotmoka.node.internal;

import io.hotmoka.beans.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.api.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * Implementation of the future of a transaction that installs a jar in a node.
 * It caches the result for repeated use.
 */
public class JarSupplierImpl implements JarSupplier {
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
	public JarSupplierImpl(TransactionReference reference, Node node) {
		this.reference = reference;
		this.node = node;
	}

	@Override
	public TransactionReference getReferenceOfRequest() {
		return reference;
	}

	@Override
	public TransactionReference get() throws TransactionRejectedException, TransactionException {
		try {
			return cachedGet != null ? cachedGet : (cachedGet = getOutcome((JarStoreTransactionResponse) node.getPolledResponse(reference)));
		}
		catch (TransactionRejectedException | TransactionException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new TransactionRejectedException(t);
		}
	}

	private TransactionReference getOutcome(JarStoreTransactionResponse response) throws TransactionException {
		if (response instanceof JarStoreTransactionFailedResponse jstfr)
			throw new TransactionException(jstfr.getClassNameOfCause(), jstfr.getMessageOfCause(), "");
		else
			return reference;
	}
}