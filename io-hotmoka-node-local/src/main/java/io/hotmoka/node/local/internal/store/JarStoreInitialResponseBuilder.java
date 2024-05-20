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

package io.hotmoka.node.local.internal.store;

import java.io.IOException;

import io.hotmoka.instrumentation.InstrumentedJars;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractInitialResponseBuilder;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * Builds the creator of response for a transaction that installs a jar in the node, during its initialization.
 */
public class JarStoreInitialResponseBuilder extends AbstractInitialResponseBuilder<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment used for computing the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	public JarStoreInitialResponseBuilder(TransactionReference reference, JarStoreInitialTransactionRequest request, ExecutionEnvironment environment) throws TransactionRejectedException, StoreException {
		super(reference, request, environment);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws StoreException, TransactionRejectedException {
		// we redefine this method, since the class loader must be able to access the
		// jar that is being installed and its dependencies, in order to instrument them
		return new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), environment, consensus);
	}

	@Override
	public JarStoreInitialTransactionResponse getResponse() throws StoreException {
		return new ResponseCreator() {

			@Override
			protected JarStoreInitialTransactionResponse body() throws ClassNotFoundException, UnsupportedVerificationVersionException, VerificationException {
				try {
					var instrumentedJar = InstrumentedJars.of(VerifiedJars.of(request.getJar(), classLoader, true, consensus.skipsVerification()), consensus.getGasCostModel());
					return TransactionResponses.jarStoreInitial(instrumentedJar.toBytes(), request.getDependencies(), consensus.getVerificationVersion());
				}
				catch (IOException t) {
					throw new RuntimeException("unexpected exception", t);
				}
			}
		}
		.create();
	}
}