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

import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * Implementation of the future of a transaction that executes a method in a node.
 * It caches the result for repeated use.
 */
public class CodeSupplierMethod implements CodeSupplier<StorageValue> {
	private final TransactionReference reference;
	private final Node node;

	/**
	 * The cached result.
	 */
	private volatile StorageValue cachedGet;

	/**
	 * Creates a future for the execution a method in a node, with a transaction request
	 * that has the given reference.
	 * 
	 * @param reference the reference to the request of the transaction
	 * @param node the node where the method is executed
	 */
	public CodeSupplierMethod(TransactionReference reference, Node node) {
		this.reference = reference;
		this.node = node;
	}

	@Override
	public TransactionReference getReferenceOfRequest() {
		return reference;
	}

	@Override
	public StorageValue get() throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		try {
			return cachedGet != null ? cachedGet : (cachedGet = getOutcome((MethodCallTransactionResponse) node.getPolledResponse(reference)));
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | NodeException | TimeoutException | InterruptedException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new TransactionRejectedException(t);
		}
	}

	private final StorageValue getOutcome(MethodCallTransactionResponse response) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		if (response instanceof MethodCallTransactionSuccessfulResponse mctsr)
			return mctsr.getResult();
		else if (response instanceof MethodCallTransactionExceptionResponse mcter)
			throw new CodeExecutionException(mcter.getClassNameOfCause(), mcter.getMessageOfCause(), mcter.getWhere());
		else if (response instanceof MethodCallTransactionFailedResponse mctfr)
			throw new TransactionException(mctfr.getClassNameOfCause(), mctfr.getMessageOfCause(), mctfr.getWhere());
		else
			return null; // void methods return no value
	}
}