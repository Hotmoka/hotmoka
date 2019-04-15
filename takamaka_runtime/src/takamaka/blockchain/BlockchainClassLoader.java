package takamaka.blockchain;

/**
 * A class loader used to access the definition of the classes
 * of Takamaka methods or constructors executed on the blockchain.
 */
public interface BlockchainClassLoader extends AutoCloseable {
	public Class<?> loadClass(String name) throws ClassNotFoundException;
	public ClassLoader getParent();
}