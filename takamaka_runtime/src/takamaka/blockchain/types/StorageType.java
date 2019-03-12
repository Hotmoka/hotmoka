package takamaka.blockchain.types;

public interface StorageType {
	Class<?> toClass() throws ClassNotFoundException;
	
	// this cannot be called compareTo since conflicts with the implicit
	// compareTo in the enum BasicTypes
	int compareAgainst(StorageType other);
}