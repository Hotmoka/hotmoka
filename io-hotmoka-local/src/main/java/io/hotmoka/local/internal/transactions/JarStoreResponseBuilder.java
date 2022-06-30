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

import java.math.BigInteger;
import java.util.logging.Level;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.responses.JarStoreNonInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.instrumentation.InstrumentedJar;
import io.hotmoka.local.EngineClassLoader;
import io.hotmoka.local.NonInitialResponseBuilder;
import io.hotmoka.local.internal.EngineClassLoaderImpl;
import io.hotmoka.local.internal.NodeInternal;
import io.hotmoka.verification.VerifiedJar;

/**
 * The creator of a response for a transaction that installs a jar in the node.
 */
public class JarStoreResponseBuilder extends NonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreNonInitialTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreResponseBuilder(TransactionReference reference, JarStoreTransactionRequest request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected EngineClassLoader mkClassLoader() {
		// we redefine this method, since the class loader must be able to access the
		// jar that is being installed and its dependencies, in order to instrument them
		return new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), node, true, consensus);
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
	protected final BigInteger gasForStoringFailedResponse() {
		BigInteger gas = request.gasLimit;
		return new JarStoreTransactionFailedResponse("placeholder for the name of the exception", "placeholder for the message of the exception", Stream.empty(), gas, gas, gas, gas).size(gasCostModel);
	}

	@Override
	public JarStoreNonInitialTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends NonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreNonInitialTransactionResponse>.ResponseCreator {
		
		private ResponseCreator() throws TransactionRejectedException {
		}

		@Override
		protected JarStoreNonInitialTransactionResponse body() {
			try {
				init();
				int jarLength = request.getJarLength();
				chargeGasForCPU(gasCostModel.cpuCostForInstallingJar(jarLength));
				chargeGasForRAM(gasCostModel.ramCostForInstallingJar(jarLength));
				VerifiedJar verifiedJar = VerifiedJar.of(request.getJar(), classLoader, false, consensus.allowsSelfCharged, consensus.skipsVerification);
				InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasCostModel);
				byte[] instrumentedBytes = instrumentedJar.toBytes();
				chargeGasForStorageOf(new JarStoreTransactionSuccessfulResponse(instrumentedBytes, request.getDependencies(), consensus.verificationVersion,  updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundPayerForAllRemainingGas();
				return new JarStoreTransactionSuccessfulResponse(instrumentedBytes, request.getDependencies(), consensus.verificationVersion, updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				logger.log(Level.INFO, "jar store failed", t);
				resetBalanceOfPayerToInitialValueMinusAllPromisedGas();
				// we do not pay back the gas
				return new JarStoreTransactionFailedResponse(t.getClass().getName(), t.getMessage(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
		}

		@Override
		public void event(Object event) {
		}
	}
}