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
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

public class JarStoreTransactionRun extends NonInitialTransactionRun<JarStoreTransactionRequest, JarStoreTransactionResponse> {

	public JarStoreTransactionRun(JarStoreTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node);
	}

	@Override
	protected EngineClassLoaderImpl mkClassLoader() throws Exception {
		return new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), this);
	}

	@Override
	protected JarStoreTransactionResponse computeResponse() throws Exception {
		Object deserializedCaller;
		UpdateOfBalance balanceUpdateInCaseOfFailure;

		try {
			deserializedCaller = deserializer.deserialize(request.caller);
			checkIsExternallyOwned(deserializedCaller);

			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new IllegalTransactionRequestException(t);
		}

		try {
			chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
			chargeForStorage(sizeCalculator.sizeOf(request));

			byte[] jar = request.getJar();
			chargeForCPU(node.getGasCostModel().cpuCostForInstallingJar(jar.length));
			chargeForRAM(node.getGasCostModel().ramCostForInstalling(jar.length));

			VerifiedJar verifiedJar = VerifiedJar.of(classLoader.jarPath(), classLoader, false);
			InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, new GasCostModelAdapter(node.getGasCostModel()));
			byte[] instrumentedBytes = instrumentedJar.toBytes();

			BigInteger balanceOfCaller = classLoader.getBalanceOf(deserializedCaller);
			StorageReference storageReferenceOfDeserializedCaller = classLoader.getStorageReferenceOf(deserializedCaller);
			UpdateOfBalance balanceUpdate = new UpdateOfBalance(storageReferenceOfDeserializedCaller, balanceOfCaller);
			JarStoreTransactionResponse response = new JarStoreTransactionSuccessfulResponse(instrumentedBytes, balanceUpdate, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			chargeForStorage(sizeCalculator.sizeOf(response));
			balanceOfCaller = increaseBalance(deserializedCaller);
			balanceUpdate = new UpdateOfBalance(storageReferenceOfDeserializedCaller, balanceOfCaller);
			return new JarStoreTransactionSuccessfulResponse(instrumentedBytes, balanceUpdate, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		catch (Throwable t) {
			// we do not pay back the gas
			BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForStorage);
			return new JarStoreTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
	}
}