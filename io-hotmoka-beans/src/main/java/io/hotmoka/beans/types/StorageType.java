package io.hotmoka.beans.types;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;

/**
 * The types that can be used in storage objects in blockchain.
 */
@Immutable
public interface StorageType {

	/**
	 * Compares this storage type with another. Puts first basic types, in their order of
	 * enumeration, then class types ordered wrt class name. This method is not
	 * called {@code compareTo} since it would conflict with the implicit {@code compareTo()}
	 * method defined in the enumeration {@link io.hotmoka.beans.types.BasicTypes}.
	 * 
	 * @param other the other type
	 * @return -1 if {@code this} comes first, 1 if {@code other} comes first, 0 if they are equal
	 */
	int compareAgainst(StorageType other);

	/**
	 * Marshals this type into a given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param context the context holding the stream
	 * @throws IOException if the type cannot be marshalled
	 */
	void into(MarshallingContext context) throws IOException;

	/**
	 * Determines if this type is eager.
	 * 
	 * @return true if and only if this type is eager
	 */
	boolean isEager();

	/**
	 * Yields the size of this type, in terms of gas units consumed in store.
	 * 
	 * @param gasCostModel the model of gas costs
	 * @return the size
	 */
	BigInteger size(GasCostModel gasCostModel);

	/**
	 * Factory method that unmarshals a type from the given stream.
	 * 
	 * @param ois the stream
	 * @return the type
	 * @throws IOException if the type could not be unmarshalled
	 * @throws ClassNotFoundException if the type could not be unmarshalled
	 */
	static StorageType from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		switch (selector) {
		case ClassType.SELECTOR:
			return new ClassType((String) ois.readObject());
		case ClassType.SELECTOR_BIGINTEGER:
			return ClassType.BIG_INTEGER;
		case ClassType.SELECTOR_STRING:
			return ClassType.STRING;
		case ClassType.SELECTOR_ACCOUNT:
			return ClassType.ACCOUNT;
		case ClassType.SELECTOR_CONTRACT:
			return ClassType.CONTRACT;
		case ClassType.SELECTOR_OBJECT:
			return ClassType.OBJECT;
		case ClassType.SELECTOR_STORAGE:
			return ClassType.STORAGE;
		case ClassType.SELECTOR_RGEOA:
			return ClassType.RGEOA;
		case ClassType.SELECTOR_MANIFEST:
			return ClassType.MANIFEST;
		case ClassType.SELECTOR_PAYABLE_CONTRACT:
			return ClassType.PAYABLE_CONTRACT;
		case ClassType.SELECTOR_STORAGE_LIST:
			return ClassType.STORAGE_LIST;
		case ClassType.SELECTOR_STORAGE_MAP:
			return ClassType.STORAGE_MAP;
		case ClassType.SELECTOR_MODIFIABLE_STORAGE_LIST_IMPL_NODE:
			return ClassType.MODIFIABLE_STORAGE_LIST_IMPL_NODE;
		case ClassType.SELECTOR_MODIFIABLE_STORAGE_MAP_IMPL_NODE:
			return ClassType.MODIFISABLE_STORAGE_MAP_IMPL_NODE;
		case ClassType.SELECTOR_EOA:
			return ClassType.EOA;
		case ClassType.SELECTOR_TEOA:
			return ClassType.TEOA;
		default:
			if (selector >= 0 && selector < 8)
				return BasicTypes.values()[selector];
			else
				throw new IOException("unexpected type selector: " + selector);
		}
	}
}