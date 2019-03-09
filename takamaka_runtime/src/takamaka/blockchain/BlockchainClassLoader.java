package takamaka.blockchain;

public interface BlockchainClassLoader extends AutoCloseable {
	public Class<?> loadClass(String name) throws ClassNotFoundException;
	public ClassLoader getParent();
}