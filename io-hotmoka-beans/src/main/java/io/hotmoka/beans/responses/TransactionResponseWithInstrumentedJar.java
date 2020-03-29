package io.hotmoka.beans.responses;

/**
 * A response for a transaction that successfully installed a jar in the blockchain.
 */
public interface TransactionResponseWithInstrumentedJar {

	/**
	 * Yields the bytes of the installed jar.
	 * 
	 * @return the bytes of the installed jar
	 */
	byte[] getInstrumentedJar();

	/**
	 * Yields the size of the instrumented jar, in bytes.
	 * 
	 * @return the size
	 */
	int getInstrumentedJarLength();
}