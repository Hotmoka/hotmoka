package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.internal.Deserializer;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

/**
 * The creator of a response for a transaction that installs a jar in the node.
 */
public class JarStoreResponseBuilder extends NonInitialResponseBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse> {

	/**
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The deserialized caller.
	 */
	private Object deserializedCaller;

	/**
	 * The jar to install, as a byte array.
	 */
	private final byte[] jar;

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreResponseBuilder(JarStoreTransactionRequest request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);

		try {
			this.jar = request.getJar();
			this.classLoader = new EngineClassLoader(jar, request.getDependencies(), node);
			chargeGasForClassLoader();
			deserializer = new Deserializer(this);
			this.deserializedCaller = deserializer.deserialize(request.caller);
			callerMustBeExternallyOwnedAccount();
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
	public final JarStoreTransactionResponse build(TransactionReference current) throws TransactionRejectedException {
		try {
			deserializedCaller = deserializer.deserialize(request.caller);
			callerAndRequestMustAgreeOnNonce();
			sellAllGasToCaller();
			increaseNonceOfCaller();

			try {
				VerifiedJar verifiedJar = VerifiedJar.of(jar, classLoader, false);
				InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasCostModel);
				byte[] instrumentedBytes = instrumentedJar.toBytes();
				chargeGasForStorage(new JarStoreTransactionSuccessfulResponse(instrumentedBytes, request.getDependencies(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				payBackAllRemainingGasToCaller();
				return new JarStoreTransactionSuccessfulResponse(instrumentedBytes, request.getDependencies(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
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