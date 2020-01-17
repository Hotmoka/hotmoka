package io.takamaka.code.blockchain.types;

import java.io.Serializable;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.annotations.Immutable;

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
	 * @throws ClassNotFoundException if some class type cannot be found
	 */
	Class<?> toClass(AbstractBlockchain blockchain) throws ClassNotFoundException;

	/**
	 * Compares this storage type with another. Puts first basic types, in their order of
	 * enumeration, then class types ordered wrt class name. This method is not
	 * called {@code compareTo} since it would conflict with the implicit {@code compareTo()}
	 * method defined in the enumeration {@link io.takamaka.code.blockchain.types.BasicTypes}.
	 * 
	 * @param other the other type
	 * @return -1 if {@code this} comes first, 1 if {@code other} comes first, 0 if they are equal
	 */
	int compareAgainst(StorageType other);
}