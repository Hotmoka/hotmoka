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
import java.util.stream.Stream;

import io.hotmoka.instrumentation.InstrumentedJars;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.HotmokaException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.VerificationException;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.responses.JarStoreTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractNonInitialResponseBuilder;
import io.hotmoka.node.local.api.ClassLoaderCreationException;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.verification.VerifiedJars;
import io.hotmoka.verification.api.IllegalJarException;

/**
 * The creator of a response for a transaction that installs a jar in the node.
 */
public class JarStoreResponseBuilder extends AbstractNonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	public JarStoreResponseBuilder(TransactionReference reference, JarStoreTransactionRequest request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws StoreException, ClassLoaderCreationException {
		// we redefine this method, since the class loader must be able to access the
		// jar that is being installed and its dependencies, in order to instrument them
		return new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), environment, consensus);
	}

	@Override
	public ResponseCreation<JarStoreTransactionResponse> getResponseCreation() throws TransactionRejectedException, StoreException, InterruptedException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends AbstractNonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse>.ResponseCreator {
		
		private ResponseCreator() throws TransactionRejectedException, StoreException {
		}

		@Override
		protected JarStoreTransactionResponse body() throws TransactionRejectedException, StoreException {
			checkConsistency();

			try {
				init();
				int jarLength = request.getJarLength();
				chargeGasForCPU(gasCostModel.cpuCostForInstallingJar(jarLength));
				chargeGasForRAM(gasCostModel.ramCostForInstallingJar(jarLength));
				var verifiedJar = VerifiedJars.of(request.getJar(), classLoader, false, _error -> {}, consensus.skipsVerification(), VerificationException::new);
				byte[] instrumentedJarBytes = InstrumentedJars.of(verifiedJar, gasCostModel).toBytes();
				chargeGasForStorageOf(TransactionResponses.jarStoreSuccessful(instrumentedJarBytes, request.getDependencies(), consensus.getVerificationVersion(), updates(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundCallerForAllRemainingGas();
				return TransactionResponses.jarStoreSuccessful(instrumentedJarBytes, request.getDependencies(), consensus.getVerificationVersion(), updates(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (HotmokaException | IllegalJarException t) {
				logFailure(t);
				return TransactionResponses.jarStoreFailed(updatesInCaseOfFailure(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty(), t.getClass().getName(), getMessageForResponse(t));
			}
		}

		@Override
		protected BigInteger minimalGasRequiredForTransaction() {
			int jarLength = request.getJarLength();

			return super.minimalGasRequiredForTransaction()
					.add(gasCostModel.cpuCostForInstallingJar(jarLength))
					.add(gasCostModel.ramCostForInstallingJar(jarLength));
		}

		@Override
		protected final int gasForStoringFailedResponse() {
			BigInteger gas = request.getGasLimit();
			return TransactionResponses.jarStoreFailed(Stream.empty(), gas, gas, gas, gas, "placeholder for the name of the exception", "placeholder for the message of the exception").size();
		}

		@Override
		public void event(Object event) {}
	}
}