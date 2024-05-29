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

package io.hotmoka.node.local.internal;

import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.instrumentation.InstrumentedJars;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.responses.JarStoreTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractNonInitialResponseBuilder;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.verification.VerifiedJars;

/**
 * The creator of a response for a transaction that installs a jar in the node.
 */
public class JarStoreResponseBuilder extends AbstractNonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse> {
	private final static Logger LOGGER = Logger.getLogger(JarStoreResponseBuilder.class.getName());

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment used for computing the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	public JarStoreResponseBuilder(TransactionReference reference, JarStoreTransactionRequest request, ExecutionEnvironment environment) throws TransactionRejectedException, StoreException {
		super(reference, request, environment);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws StoreException, TransactionRejectedException {
		// we redefine this method, since the class loader must be able to access the
		// jar that is being installed and its dependencies, in order to instrument them
		return new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), environment, consensus);
	}

	@Override
	protected BigInteger minimalGasRequiredForTransaction() {
		int jarLength = request.getJarLength();
		BigInteger result = super.minimalGasRequiredForTransaction();
		result = result.add(gasCostModel.cpuCostForInstallingJar(jarLength));
		result = result.add(gasCostModel.ramCostForInstallingJar(jarLength));

		return result;
	}

	@Override
	protected final int gasForStoringFailedResponse() {
		BigInteger gas = request.getGasLimit();
		return TransactionResponses.jarStoreFailed("placeholder for the name of the exception", "placeholder for the message of the exception", Stream.empty(), gas, gas, gas, gas).size();
	}

	@Override
	public JarStoreTransactionResponse getResponse() throws StoreException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends AbstractNonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse>.ResponseCreator {
		
		private ResponseCreator() {
		}

		@Override
		protected JarStoreTransactionResponse body() {
			try {
				init();
				int jarLength = request.getJarLength();
				chargeGasForCPU(gasCostModel.cpuCostForInstallingJar(jarLength));
				chargeGasForRAM(gasCostModel.ramCostForInstallingJar(jarLength));
				var verifiedJar = VerifiedJars.of(request.getJar(), classLoader, false, consensus.skipsVerification());
				var instrumentedJar = InstrumentedJars.of(verifiedJar, gasCostModel);
				var instrumentedBytes = instrumentedJar.toBytes();
				chargeGasForStorageOf(TransactionResponses.jarStoreSuccessful(instrumentedBytes, request.getDependencies(), consensus.getVerificationVersion(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundPayerForAllRemainingGas();
				return TransactionResponses.jarStoreSuccessful(instrumentedBytes, request.getDependencies(), consensus.getVerificationVersion(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				LOGGER.log(Level.INFO, "jar store failed", t);
				resetBalanceOfPayerToInitialValueMinusAllPromisedGas();
				// we do not pay back the gas
				try {
					return TransactionResponses.jarStoreFailed(t.getClass().getName(), t.getMessage(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
				}
				catch (UpdatesExtractionException | StoreException e) {
					throw new RuntimeException(e); // TODO
				}
			}
		}

		@Override
		public void event(Object event) {
		}
	}
}