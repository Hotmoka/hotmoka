package takamaka.blockchain.types;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.lang.Immutable;

/**
 * The types that can be used in storage objects in blockchain.
 */
@Immutable
public interface StorageType {

	/**
	 * Yields the class object that represents this type, for the current
	 * transaction of the given blockchain.
	 * 
	 * @param blockchain the blockchain that is executing the transaction
	 * @return the class object, if any
	 * @throws ClassNotFoundException if some class type cannot be found for that transaction
	 */
	Class<?> toClass(AbstractBlockchain blockchain) throws ClassNotFoundException;

	/**
	 * Compares this storage type with another. Puts first basic types, in their order of
	 * enumeration, then class types ordered wrt class name. This method is not
	 * called {@code compareTo} since it would conflict with the implicit {@code compareTo}
	 * method defined in the enumeration {@link takamaka.blockchain.types.BasicTypes}.
	 * 
	 * @param other the other type
	 * @return -1 if {@code this} comes first, 1 if {@code other} comes first, 0 if they are equal
	 */
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

	/**
	 * Determines if a field of this type of a storage object is not loaded
	 * at deserialization time, but only when and if is accessed.
	 * 
	 * @return true if and only if that condition holds
	 */
	boolean isLazy();
}