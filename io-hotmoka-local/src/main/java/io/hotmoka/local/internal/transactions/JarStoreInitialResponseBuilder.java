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

package io.hotmoka.local.internal.transactions;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.local.EngineClassLoader;
import io.hotmoka.local.InitialResponseBuilder;
import io.hotmoka.local.internal.EngineClassLoaderImpl;
import io.hotmoka.local.internal.NodeInternal;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerificationException;
import io.takamaka.code.verification.VerifiedJar;

/**
 * Builds the creator of response for a transaction that installs a jar in the node, during its initialization.
 */
public class JarStoreInitialResponseBuilder extends InitialResponseBuilder<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreInitialResponseBuilder(TransactionReference reference, JarStoreInitialTransactionRequest request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected EngineClassLoader mkClassLoader() {
		// we redefine this method, since the class loader must be able to access the
		// jar that is being installed and its dependencies, in order to instrument them
		return new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), node, true, consensus);
	}

	@Override
	public JarStoreInitialTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator() {

			@Override
			protected JarStoreInitialTransactionResponse body() {
				try {
					InstrumentedJar instrumentedJar = InstrumentedJar.of(VerifiedJar.of(request.getJar(), classLoader, true, consensus.allowsSelfCharged, consensus.skipsVerification), node.getGasCostModel());
					return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes(), request.getDependencies(), consensus.verificationVersion);
				}
				catch (VerificationException e) {
					throw e;
				}
				catch (Throwable t) {
					throw InternalFailureException.of(t);
				}
			}
		}
		.create();
	}
}