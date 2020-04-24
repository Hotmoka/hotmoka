package io.hotmoka.beans.types;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import io.hotmoka.beans.annotations.Immutable;

/**
 * The types that can be used in storage objects in blockchain.
 */
@Immutable
public interface StorageType extends Serializable {

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
	 * Marshals this type into the given stream. This method
	 * in general performs better than standard Java serialization, wrt the size
	 * of the marshalled data.
	 * 
	 * @param oos the stream
	 * @throws IOException if the type cannot be marshalled
	 */
	void into(ObjectOutputStream oos) throws IOException;

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
		if (selector < 8)
			return BasicTypes.values()[selector];
		else if (selector == 8)
			return new ClassType(ois.readUTF());
		else
			throw new IOException("unexpected type selector: " + selector);
	}
}