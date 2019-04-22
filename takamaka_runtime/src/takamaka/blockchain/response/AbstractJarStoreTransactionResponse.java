package takamaka.blockchain.response;

/**
 * A response for a transaction that successfully installed a jar in the blockchain.
 */
public interface AbstractJarStoreTransactionResponse {

	/**
	 * Yields the bytes of the installed jar.
	 * 
	 * @return the bytes of the installed jar
	 */
	public byte[] getInstrumentedJar();
}