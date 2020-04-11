package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

/**
 * Builds the creator of a transaction that installs a jar in the node.
 */
public class JarStoreTransactionBuilder extends NonInitialTransactionBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse> {

	/**
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The deserialized caller.
	 */
	private final Object deserializedCaller;

	/**
	 * The jar to install, as a byte array.
	 */
	private final byte[] jar;

	/**
	 * Creates the builder of a transaction that installs a jar in the node.
	 * 
	 * @param request the request of the transaction
	 * @param transaction the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreTransactionBuilder(JarStoreTransactionRequest request, TransactionReference transaction, Node node) throws TransactionRejectedException {
		super(request, transaction, node);

		try {
			this.jar = request.getJar();
			this.classLoader = new EngineClassLoader(jar, transaction, request.getDependencies(), this);
			this.deserializedCaller = deserializer.deserialize(request.caller);
			callerMustBeExternallyOwnedAccount();
			callerAndRequestMustAgreeOnNonce();
			callerMustBeAbleToPayForAllGas();
			chargeGasForCPU(gasCostModel.cpuBaseTransactionCost());
			chargeGasForStorageOfRequest();
			chargeGasForCPU(gasCostModel.cpuCostForInstallingJar(jar.length));
			chargeGasForRAM(gasCostModel.ramCostForInstallingJar(jar.length));
			remainingGasMustBeEnoughForStoringFailedResponse();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final JarStoreTransactionResponse build() throws TransactionRejectedException {
		try {
			sellAllGasToCaller();
			increaseNonceOfCaller();

			try {
				VerifiedJar verifiedJar = VerifiedJar.of(jar, classLoader, false);
				InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasCostModel);
				byte[] instrumentedBytes = instrumentedJar.toBytes();
				chargeGasForStorage(new JarStoreTransactionSuccessfulResponse(instrumentedBytes, updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				payBackAllRemainingGasToCaller();
				return new JarStoreTransactionSuccessfulResponse(instrumentedBytes, updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				// we do not pay back the gas
				return new JarStoreTransactionFailedResponse(t.getClass().getName(), t.getMessage(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	protected Object getDeserializedCaller() {
		return deserializedCaller;
	}

	@Override
	protected final BigInteger gasForStoringFailedResponse() {
		BigInteger gas = gas();

		return sizeCalculator.sizeOfResponse(new JarStoreTransactionFailedResponse("placeholder for the name of the exception", "placeholder for the message of the exception", updatesToBalanceOrNonceOfCaller(), gas, gas, gas, gas));
	}
}