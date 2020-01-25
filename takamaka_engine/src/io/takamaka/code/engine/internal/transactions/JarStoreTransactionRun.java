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
import io.takamaka.code.engine.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.engine.internal.TempJarFile;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

public class JarStoreTransactionRun extends AbstractTransactionRun<JarStoreTransactionRequest, JarStoreTransactionResponse> {

	public JarStoreTransactionRun(JarStoreTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node, request.gas);
	}

	@Override
	protected JarStoreTransactionResponse computeResponse() throws Exception {
		try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
			this.classLoader = classLoader;
			Object deserializedCaller = deserializer.deserialize(request.caller);
			checkIsExternallyOwned(deserializedCaller);

			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);

			// before this line, an exception will abort the transaction and leave the blockchain unchanged;
			// after this line, the transaction will be added to the blockchain, possibly as a failed one

			try {
				chargeForCPU(getGasCostModel().cpuBaseTransactionCost());
				chargeForStorage(sizeCalculator.sizeOf(request));

				byte[] jar = request.getJar();
				chargeForCPU(getGasCostModel().cpuCostForInstallingJar(jar.length));
				chargeForRAM(getGasCostModel().ramCostForInstalling(jar.length));

				byte[] instrumentedBytes;
				// we transform the array of bytes into a real jar file
				try (TempJarFile original = new TempJarFile(jar);
					 EngineClassLoader jarClassLoader = new EngineClassLoader(original.toPath(), request.getDependencies(), this)) {
					VerifiedJar verifiedJar = VerifiedJar.of(original.toPath(), jarClassLoader, false);
					InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasModelAsForInstrumentation());
					instrumentedBytes = instrumentedJar.toBytes();
				}

				BigInteger balanceOfCaller = getBalanceOf(deserializedCaller);
				StorageReference storageReferenceOfDeserializedCaller = getStorageReferenceOf(deserializedCaller);
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
}