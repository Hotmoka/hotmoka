package io.hotmoka.beans.responses;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.updates.Update;

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
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreTransactionSuccessfulResponse(byte[] instrumentedJar, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.instrumentedJar = instrumentedJar.clone();
	}

	@Override
	public byte[] getInstrumentedJar() {
		return instrumentedJar.clone();
	}

	@Override
	public int getInstrumentedJarLength() {
		return instrumentedJar.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: instrumentedJar)
            sb.append(String.format("%02x", b));

        return super.toString() + "\n  instrumented jar: " + sb.toString();
	}

	@Override
	public TransactionReference getOutcomeAt(TransactionReference transactionReference) {
		// the outcome is the reference to the transaction where this response has been executed
		return transactionReference;
	}
}