package takamaka.blockchain.types;

public interface StorageType {
	Class<?> toClass() throws ClassNotFoundException;
}