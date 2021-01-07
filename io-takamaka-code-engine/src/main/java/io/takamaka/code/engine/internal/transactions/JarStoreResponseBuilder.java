package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreNonInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.takamaka.code.engine.AbstractLocalNode;
import io.takamaka.code.engine.EngineClassLoader;
import io.takamaka.code.engine.NonInitialResponseBuilder;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

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
	public JarStoreResponseBuilder(TransactionReference reference, JarStoreTransactionRequest request, AbstractLocalNode<?,?> node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws Exception {
		// we redefine this method, since the class loader must be able to access the
		// jar that is being installed and its dependencies, in order to instrument them
		return new EngineClassLoader(request.getJar(), request.getDependencies(), node);
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
				VerifiedJar verifiedJar = VerifiedJar.of(request.getJar(), classLoader, verificationVersion,false, node.config.allowSelfCharged);
				InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasCostModel);
				byte[] instrumentedBytes = instrumentedJar.toBytes();
				chargeGasForStorageOf(new JarStoreTransactionSuccessfulResponse(instrumentedBytes, request.getDependencies(), verificationVersion, updatesToBalanceOrNonceOfCallerOrValidators(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				refundPayerForAllRemainingGas();
				sendAllConsumedGasToValidators();
				return new JarStoreTransactionSuccessfulResponse(instrumentedBytes, request.getDependencies(), verificationVersion, updatesToBalanceOrNonceOfCallerOrValidators(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				sendAllConsumedGasToValidatorsIncludingPenalty();
				// we do not pay back the gas
				return new JarStoreTransactionFailedResponse(t.getClass().getName(), t.getMessage(), updatesToBalanceOrNonceOfCallerOrValidators(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
		}

		@Override
		public void event(Object event) {
		}
	}
}