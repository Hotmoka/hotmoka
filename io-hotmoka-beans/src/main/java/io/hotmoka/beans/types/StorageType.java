/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.beans.types;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.takamaka.code.constants.Constants;

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
	 * @param context the unmarshalling context
	 * @return the type
	 * @throws IOException if the type could not be unmarshalled
     */
	static StorageType from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();
		switch (selector) {
		case ClassType.SELECTOR:
			return new ClassType(context.readStringShared());
		case ClassType.SELECTOR_BIGINTEGER:
			return ClassType.BIG_INTEGER;
		case ClassType.SELECTOR_ERC20:
			return ClassType.ERC20;
		case ClassType.SELECTOR_IERC20:
			return ClassType.IERC20;
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
		case ClassType.SELECTOR_MANIFEST:
			return ClassType.MANIFEST;
		case ClassType.SELECTOR_GAS_STATION:
			return ClassType.GAS_STATION;
		case ClassType.SELECTOR_PAYABLE_CONTRACT:
			return ClassType.PAYABLE_CONTRACT;
		case ClassType.SELECTOR_STORAGE_LIST:
			return ClassType.STORAGE_LIST;
		case ClassType.SELECTOR_STORAGE_MAP:
			return ClassType.STORAGE_MAP;
		case ClassType.SELECTOR_STORAGE_TREE_ARRAY:
			return ClassType.STORAGE_TREE_ARRAY;
		case ClassType.SELECTOR_STORAGE_TREE_ARRAY_NODE:
			return ClassType.STORAGE_TREE_ARRAY_NODE;
		case ClassType.SELECTOR_STORAGE_TREE_INTMAP_NODE:
			return ClassType.STORAGE_TREE_INTMAP_NODE;
		case ClassType.SELECTOR_STORAGE_TREE_SET:
			return ClassType.STORAGE_TREE_SET;
		case ClassType.SELECTOR_STORAGE_TREE_MAP:
			return ClassType.STORAGE_TREE_MAP;
		case ClassType.SELECTOR_STORAGE_TREE_MAP_BLACK_NODE:
			return ClassType.STORAGE_TREE_MAP_BLACK_NODE;
		case ClassType.SELECTOR_STORAGE_TREE_MAP_RED_NODE:
			return ClassType.STORAGE_TREE_MAP_RED_NODE;
		case ClassType.SELECTOR_STORAGE_LINKED_LIST_NODE:
			return ClassType.STORAGE_LINKED_LIST_NODE;
		case ClassType.SELECTOR_STORAGE_TREE_MAP_NODE:
			return ClassType.STORAGE_TREE_MAP_NODE;
		case ClassType.SELECTOR_EOA:
			return ClassType.EOA;
		case ClassType.SELECTOR_UNSIGNED_BIG_INTEGER:
			return ClassType.UNSIGNED_BIG_INTEGER;
		case ClassType.SELECTOR_GAS_PRICE_UPDATE:
			return ClassType.GAS_PRICE_UPDATE;
		case ClassType.SELECTOR_GENERIC_GAS_STATION:
			return ClassType.GENERIC_GAS_STATION;
		case ClassType.SELECTOR_EVENT:
			return ClassType.EVENT;
		case ClassType.SELECTOR_IO_TAKAMAKA_CODE:
			return new ClassType(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME + context.readStringShared());
		case ClassType.SELECTOR_IO_TAKAMAKA_CODE_LANG:
			return new ClassType(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + context.readStringShared());
		case ClassType.SELECTOR_IO_TAKAMAKA_CODE_UTIL:
			return new ClassType(Constants.IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME + context.readStringShared());
		case ClassType.SELECTOR_IO_TAKAMAKA_CODE_TOKENS:
			return new ClassType(Constants.IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME + context.readStringShared());
		default:
			if (selector >= 0 && selector < 8)
				return BasicTypes.values()[selector];
			else
				throw new IOException("unexpected type selector: " + selector);
		}
	}

	/**
	 * Yields the storage type corresponding to the given class.
	 * 
	 * @param clazz the class
	 * @return the storage type
	 */
	static StorageType of(Class<?> clazz) {
		if (clazz == boolean.class)
			return BasicTypes.BOOLEAN;
		else if (clazz == byte.class)
			return BasicTypes.BYTE;
		else if (clazz == char.class)
			return BasicTypes.CHAR;
		else if (clazz == short.class)
			return BasicTypes.SHORT;
		else if (clazz == int.class)
			return BasicTypes.INT;
		else if (clazz == long.class)
			return BasicTypes.LONG;
		else if (clazz == float.class)
			return BasicTypes.FLOAT;
		else if (clazz == double.class)
			return BasicTypes.DOUBLE;
		else
			return new ClassType(clazz.getName());
	}
}