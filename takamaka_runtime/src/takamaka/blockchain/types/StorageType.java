package takamaka.blockchain.types;

import java.io.Serializable;
import java.math.BigInteger;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.lang.Immutable;

/**
 * The types that can be used in storage objects in blockchain.
 */
@Immutable
public interface StorageType extends Serializable {

	/**
	 * Yields the class object that represents this type in the Java language,
	 * for the current transaction of the given blockchain.
	 * 
	 * @param blockchain the blockchain that is executing the transaction
	 * @return the class object, if any
	 * @throws ClassNotFoundException if some class type cannot be found for that transaction
	 */
	Class<?> toClass(AbstractBlockchain blockchain) throws ClassNotFoundException;

	/**
	 * Compares this storage type with another. Puts first basic types, in their order of
	 * enumeration, then class types ordered wrt class name. This method is not
	 * called {@code compareTo} since it would conflict with the implicit {@code compareTo()}
	 * method defined in the enumeration {@link takamaka.blockchain.types.BasicTypes}.
	 * 
	 * @param other the other type
	 * @return -1 if {@code this} comes first, 1 if {@code other} comes first, 0 if they are equal
	 */
	int compareAgainst(StorageType other);

	/**
	 * The size of this type, in terms of storage gas units consumed if it is stored in blockchain.
	 * 
	 * @return the size of this type
	 */
	BigInteger size();
}