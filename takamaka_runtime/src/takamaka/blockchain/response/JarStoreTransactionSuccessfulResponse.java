package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.Set;

import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

/**
 * A response for a successful transaction that installs a jar in an initialized blockchain.
 */
@Immutable
public class JarStoreTransactionSuccessfulResponse extends JarStoreTransactionResponse {

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
	 * @param consumedGas the amount of gas consumed by the transaction
	 */
	public JarStoreTransactionSuccessfulResponse(byte[] instrumentedJar, Set<Update> updates, BigInteger consumedGas) {
		super(updates, consumedGas);

		this.instrumentedJar = instrumentedJar.clone();
	}

	/**
	 * Yields the bytes of the jar to install.
	 * 
	 * @return the bytes of the jar to install
	 */
	public byte[] getInstrumentedJar() {
		return instrumentedJar.clone();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        for (byte b: instrumentedJar)
            sb.append(String.format("%02x", b));

        return super.toString()
			+ "\n  instrumented jar: " + sb.toString();
	}
}