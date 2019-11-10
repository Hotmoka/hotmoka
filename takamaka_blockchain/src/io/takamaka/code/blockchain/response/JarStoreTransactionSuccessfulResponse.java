package io.takamaka.code.blockchain.response;

import java.math.BigInteger;

import io.takamaka.code.blockchain.GasCosts;
import io.takamaka.code.blockchain.UpdateOfBalance;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A response for a successful transaction that installs a jar in a blockchain.
 */
@Immutable
public class JarStoreTransactionSuccessfulResponse extends JarStoreTransactionResponse implements TransactionResponseWithInstrumentedJar {

	private static final long serialVersionUID = -8888957484092351352L;

	/**
	 * The bytes of the jar to install, instrumented.
	 */
	private final byte[] instrumentedJar;

	/**
	 * Builds the transaction response.
	 * 
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 * @param callerBalanceUpdate the update of balance of the caller of the transaction, for paying for the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreTransactionSuccessfulResponse(byte[] instrumentedJar, UpdateOfBalance callerBalanceUpdate, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(callerBalanceUpdate, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.instrumentedJar = instrumentedJar.clone();
	}

	@Override
	public byte[] getInstrumentedJar() {
		return instrumentedJar.clone();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: instrumentedJar)
            sb.append(String.format("%02x", b));

        return super.toString() + "\n  instrumented jar: " + sb.toString();
	}

	@Override
	public BigInteger size() {
		return super.size().add(GasCosts.storageCostForInstalling(instrumentedJar));
	}
}