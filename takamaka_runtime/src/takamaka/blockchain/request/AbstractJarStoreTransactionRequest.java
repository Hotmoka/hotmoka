package takamaka.blockchain.request;

import java.util.stream.Stream;

import takamaka.blockchain.Classpath;
import takamaka.lang.Immutable;

/**
 * A request for a transaction that installs a jar in a blockchain.
 */
@Immutable
public interface AbstractJarStoreTransactionRequest {
	
	/**
	 * Yields the bytes of the jar to install.
	 * 
	 * @return the bytes of the jar to install
	 */
	public byte[] getJar();

	/**
	 * Yields the dependencies of the jar to install.
	 * 
	 * @return the dependencies
	 */
	public Stream<Classpath> getDependencies();
}