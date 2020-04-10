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
	 * The response computed at the end of the transaction.
	 */
	private final JarStoreTransactionResponse response;


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
			byte[] jar = request.getJar();
			this.classLoader = new EngineClassLoader(jar, transaction, request.getDependencies(), this);
			this.deserializedCaller = deserializer.deserialize(request.caller);
			callerMustBeAnExternallyOwnedAccount();
			nonceOfCallerMustMatch(request);

			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			chargeToCallerMinimalGasFor(request);

			JarStoreTransactionResponse response;
			try {
				chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
				chargeForStorage(request);
				chargeForCPU(node.getGasCostModel().cpuCostForInstallingJar(jar.length));
				chargeForRAM(node.getGasCostModel().ramCostForInstalling(jar.length));

				VerifiedJar verifiedJar = VerifiedJar.of(jar, classLoader, false);
				InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, node.getGasCostModel());
				byte[] instrumentedBytes = instrumentedJar.toBytes();

				chargeForStorage(new JarStoreTransactionSuccessfulResponse(instrumentedBytes, updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				payBackRemainingGas();
				setNonceAfter(request);
				response = new JarStoreTransactionSuccessfulResponse(instrumentedBytes, updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				setNonceAfter(request);
				// we do not pay back the gas
				response = new JarStoreTransactionFailedResponse(t.getClass().getName(), t.getMessage(), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}

			this.response = response;
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
	public final JarStoreTransactionResponse getResponse() {
		return response;
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