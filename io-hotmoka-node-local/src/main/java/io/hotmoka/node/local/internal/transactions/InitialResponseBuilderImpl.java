/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.node.local.internal.transactions;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.InitialTransactionRequest;
import io.hotmoka.node.api.responses.InitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.StoreTransaction;

/**
 * Implementation of the creator of the response for an initial transaction. Initial transactions do not consume gas.
 * 
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class InitialResponseBuilderImpl<Request extends InitialTransactionRequest<Response>, Response extends InitialTransactionResponse> extends AbstractResponseBuilder<Request, Response> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected InitialResponseBuilderImpl(TransactionReference reference, Request request, StoreTransaction<?,?> storeTransaction) throws TransactionRejectedException {
		super(reference, request, storeTransaction);

		try {
			if (storeTransaction.getManifest().isPresent())
				throw new TransactionRejectedException("Cannot run an initial transaction request in an already initialized node");
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	protected abstract class ResponseCreator extends AbstractResponseBuilder<Request, Response>.ResponseCreator {

		protected ResponseCreator() throws TransactionRejectedException {
		}

		@Override
		public final void chargeGasForCPU(BigInteger amount) {
			// initial transactions consume no gas; this implementation is needed
			// since code run in initial transactions (such as the creation of gametes) tries to charge for gas
		}

		@Override
		public final void chargeGasForRAM(BigInteger amount) {
			// initial transactions consume no gas; this implementation is needed
			// since code run in initial transactions (such as the creation of gametes) tries to charge for gas
		}

		@Override
		public final void event(Object event) {
			// initial transactions do not generate events
		}

		@Override
		public final <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
			// initial transactions consume no gas; this implementation is needed
			// if (in the future) code run in initial transactions tries to run tasks with a limited amount of gas
			return what.call();
		}
	}
}