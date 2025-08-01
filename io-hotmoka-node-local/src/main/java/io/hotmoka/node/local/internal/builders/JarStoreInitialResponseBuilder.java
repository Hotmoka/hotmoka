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

import java.util.logging.Level;

import io.hotmoka.instrumentation.InstrumentedJars;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractInitialResponseBuilder;
import io.hotmoka.node.local.HotmokaTransactionException;
import io.hotmoka.node.local.IllegalJarException;
import io.hotmoka.node.local.UnknownTypeException;
import io.hotmoka.node.local.VerificationException;
import io.hotmoka.node.local.api.ClassLoaderCreationException;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.verification.VerifiedJars;

/**
 * Builds the creator of response for a transaction that installs a jar in the node, during its initialization.
 */
public class JarStoreInitialResponseBuilder extends AbstractInitialResponseBuilder<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	public JarStoreInitialResponseBuilder(TransactionReference reference, JarStoreInitialTransactionRequest request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws ClassLoaderCreationException {
		// we redefine this method, since the class loader must be able to access the
		// jar that is being installed and its dependencies, in order to instrument them
		return new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), environment, consensus);
	}

	@Override
	public ResponseCreation<JarStoreInitialTransactionResponse> getResponseCreation() throws TransactionRejectedException, InterruptedException {
		return new ResponseCreator() {

			@Override
			protected JarStoreInitialTransactionResponse body() throws TransactionRejectedException {
				checkConsistency();

				try {
					var verifiedJar = VerifiedJars.of(request.getJar(), classLoader, true, _error -> {}, consensus.skipsVerification(), VerificationException::new, IllegalJarException::new, UnknownTypeException::new);
					byte[] instrumentedJarBytes = InstrumentedJars.of(verifiedJar, consensus.getGasCostModel(), IllegalJarException::new, UnknownTypeException::new).toBytes();
					return TransactionResponses.jarStoreInitial(instrumentedJarBytes, request.getDependencies(), consensus.getVerificationVersion());
				}
				catch (HotmokaTransactionException e) {
					logFailure(Level.SEVERE, e);
					throw new TransactionRejectedException(e);
				}
			}
		}
		.create();
	}
}