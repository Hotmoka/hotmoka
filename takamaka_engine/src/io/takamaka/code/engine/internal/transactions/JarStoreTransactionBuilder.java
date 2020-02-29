package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.values.StorageReference;
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
	 * The response computed at the end of the transaction.
	 */
	private final JarStoreTransactionResponse response;

	/**
	 * Builds the creator of a transaction that installs a jar in the node.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionException if the transaction cannot be created
	 */
	public JarStoreTransactionBuilder(JarStoreTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		try (EngineClassLoader classLoader = new EngineClassLoader(request.getJar(), request.getDependencies(), this)) {
			this.classLoader = classLoader;
			Object deserializedCaller = deserializer.deserialize(request.caller);
			checkIsExternallyOwned(deserializedCaller);
			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);

			JarStoreTransactionResponse response;
			try {
				chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
				chargeForStorage(request);

				byte[] jar = request.getJar();
				chargeForCPU(node.getGasCostModel().cpuCostForInstallingJar(jar.length));
				chargeForRAM(node.getGasCostModel().ramCostForInstalling(jar.length));

				VerifiedJar verifiedJar = VerifiedJar.of(classLoader.jarPath(), classLoader, false);
				InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, node.getGasCostModel());
				byte[] instrumentedBytes = instrumentedJar.toBytes();

				BigInteger balanceOfCaller = classLoader.getBalanceOf(deserializedCaller);
				StorageReference storageReferenceOfDeserializedCaller = classLoader.getStorageReferenceOf(deserializedCaller);
				UpdateOfBalance balanceUpdate = new UpdateOfBalance(storageReferenceOfDeserializedCaller, balanceOfCaller);
				chargeForStorage(new JarStoreTransactionSuccessfulResponse(instrumentedBytes, balanceUpdate, gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				balanceOfCaller = increaseBalance(deserializedCaller);
				balanceUpdate = new UpdateOfBalance(storageReferenceOfDeserializedCaller, balanceOfCaller);
				response = new JarStoreTransactionSuccessfulResponse(instrumentedBytes, balanceUpdate, gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				// we do not pay back the gas
				response = new JarStoreTransactionFailedResponse(wrapAsTransactionException(t), balanceUpdateInCaseOfFailure, gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}

			this.response = response;
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t);
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
}