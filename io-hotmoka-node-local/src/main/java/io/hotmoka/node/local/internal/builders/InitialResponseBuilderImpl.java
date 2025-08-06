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

package io.hotmoka.node.local.internal.builders;

import java.math.BigInteger;
import java.util.logging.Level;

import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.InitialTransactionRequest;
import io.hotmoka.node.api.responses.InitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;

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
	 * @param environment the execution environment where the response is built
	 */
	protected InitialResponseBuilderImpl(TransactionReference reference, Request request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	protected abstract class ResponseCreator extends AbstractResponseBuilder<Request, Response>.ResponseCreator {

		protected ResponseCreator() throws TransactionRejectedException {}

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

		/**
		 * Checks if the request should be rejected, even before trying to execute it.
		 * 
		 * @throws TransactionRejectedException if the request should be rejected
		 */
		protected void checkBeforeExecution() throws TransactionRejectedException {
			if (environment.getManifest().isPresent())
				throw new TransactionRejectedException("Cannot run an initial transaction request in an already initialized node");
		}

		/**
		 * Checks if the request should be rejected, even before trying to execute it.
		 * 
		 * @throws TransactionRejectedException if the request should be rejected
		 */
		protected final void checkConsistency() throws TransactionRejectedException {
			super.checkConsistency();

			try {
				checkBeforeExecution();
			}
			catch (TransactionRejectedException e) {
				logFailure(Level.SEVERE, e);
				throw e;
			}
		}
	}
}