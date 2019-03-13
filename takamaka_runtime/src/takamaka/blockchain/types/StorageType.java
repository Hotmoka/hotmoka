package takamaka.blockchain.types;

public interface StorageType {
	Class<?> toClass(ClassLoader classLoader) throws ClassNotFoundException;
	
	// this cannot be called compareTo since conflicts with the implicit
	// compareTo in the enum BasicTypes
	int compareAgainst(StorageType other);

	/**
	 * Yields a storage type from its string representation. It always hold that
	 * {@code of(type.toString()).equals(type)}.
	 * 
	 * @param s the string representation
	 * @return the corresponding storage type
	 */
	static StorageType of(String s) {
		switch (s) {
		case "boolean": return BasicTypes.BOOLEAN;
		case "byte": return BasicTypes.BYTE;
		case "char": return BasicTypes.CHAR;
		case "double": return BasicTypes.DOUBLE;
		case "float": return BasicTypes.FLOAT;
		case "int": return BasicTypes.INT;
		case "long": return BasicTypes.LONG;
		case "short": return BasicTypes.SHORT;
		default: return new ClassType(s);
		}
	}
}