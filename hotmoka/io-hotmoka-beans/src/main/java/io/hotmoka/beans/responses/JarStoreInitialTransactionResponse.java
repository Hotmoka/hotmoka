package io.hotmoka.beans.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.InitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;

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
}