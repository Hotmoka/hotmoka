package io.hotmoka.beans.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;

/**
 * A response for a transaction that installs a jar in a yet not initialized blockchain.
 */
@Immutable
public class JarStoreInitialTransactionResponse implements InitialTransactionResponse, TransactionResponseWithInstrumentedJar {

	private static final long serialVersionUID = 7320005929052884412L;

	/**
	 * The bytes of the jar to install, instrumented.
	 */
	private final byte[] instrumentedJar;

	/**
	 * Builds the transaction response.
	 * 
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 */
	public JarStoreInitialTransactionResponse(byte[] instrumentedJar) {
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

        return getClass().getSimpleName() + ":\n  instrumented jar: " + sb.toString();
	}

	/**
	 * Yields the outcome of the execution having this response, performed
	 * at the given transaction reference.
	 * 
	 * @param transactionReference the transaction reference
	 * @return the outcome
	 */
	public TransactionReference getOutcomeAt(TransactionReference transactionReference) {
		// the result of installing a jar in a node is the reference to the transaction that installed the jar
		return transactionReference;
	}
}