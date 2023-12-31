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
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.constants.Constants;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * A class type that can be used for stored objects in blockchain.
 */
@Immutable
public final class ClassType extends AbstractMarshallable implements StorageType {
	final static byte SELECTOR = 8;
	final static byte SELECTOR_IO_TAKAMAKA_CODE = 9;
	final static byte SELECTOR_IO_TAKAMAKA_CODE_LANG = 10;
	final static byte SELECTOR_IO_TAKAMAKA_CODE_UTIL = 11;
	final static byte SELECTOR_IO_TAKAMAKA_CODE_TOKENS = 34;
	final static byte SELECTOR_STORAGE_LIST = 12;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE = 13;
	final static byte SELECTOR_STORAGE_LINKED_LIST_NODE = 14;
	final static byte SELECTOR_EOA = 15;
	final static byte SELECTOR_STRING = 17;
	final static byte SELECTOR_ACCOUNT = 18;
	final static byte SELECTOR_MANIFEST = 19;
	final static byte SELECTOR_CONTRACT = 20;
	final static byte SELECTOR_OBJECT = 22;
	final static byte SELECTOR_STORAGE = 23;
	final static byte SELECTOR_GENERIC_GAS_STATION = 24;
	final static byte SELECTOR_EVENT = 25;
	final static byte SELECTOR_BIGINTEGER = 26;
	final static byte SELECTOR_PAYABLE_CONTRACT = 27;
	final static byte SELECTOR_STORAGE_MAP_VIEW = 28;
	final static byte SELECTOR_STORAGE_TREE_MAP = 29;
	final static byte SELECTOR_STORAGE_TREE_MAP_BLACK_NODE = 30;
	final static byte SELECTOR_STORAGE_TREE_MAP_RED_NODE = 31;
	final static byte SELECTOR_UNSIGNED_BIG_INTEGER = 32;
	final static byte SELECTOR_ERC20 = 33;
	final static byte SELECTOR_IERC20 = 35;
	final static byte SELECTOR_STORAGE_TREE_ARRAY = 36;
	final static byte SELECTOR_STORAGE_TREE_ARRAY_NODE = 37;
	final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE = 38;
	final static byte SELECTOR_STORAGE_TREE_SET = 39;
	final static byte SELECTOR_GAS_STATION = 40;
	final static byte SELECTOR_GAS_PRICE_UPDATE = 16;

	/**
	 * The name of the class type.
	 */
	private final String name;

	/**
	 * Builds a class type that can be used for storage objects in blockchain.
	 * 
	 * @param name the name of the class
	 */
	ClassType(String name) {
		Objects.requireNonNull(name, "name cannot be null");
		this.name = name;
	}

	/**
	 * Yields the fully-qualified name of this class.
	 * 
	 * @return the fully-qualified name
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassType ct && ct.name.equals(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public int compareAgainst(StorageType other) {
		if (other instanceof ClassType ct)
			return name.compareTo(ct.name);
		else // other instanceof BasicTypes
			return 1;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (equals(StorageTypes.BIG_INTEGER))
			context.writeByte(SELECTOR_BIGINTEGER);
		else if (equals(StorageTypes.UNSIGNED_BIG_INTEGER))
			context.writeByte(SELECTOR_UNSIGNED_BIG_INTEGER);
		else if (equals(StorageTypes.GAS_PRICE_UPDATE))
			context.writeByte(SELECTOR_GAS_PRICE_UPDATE);
		else if (equals(StorageTypes.ERC20))
			context.writeByte(SELECTOR_ERC20);
		else if (equals(StorageTypes.IERC20))
			context.writeByte(SELECTOR_IERC20);
		else if (equals(StorageTypes.STRING))
			context.writeByte(SELECTOR_STRING);
		else if (equals(StorageTypes.ACCOUNT))
			context.writeByte(SELECTOR_ACCOUNT);
		else if (equals(StorageTypes.MANIFEST))
			context.writeByte(SELECTOR_MANIFEST);
		else if (equals(StorageTypes.GAS_STATION))
			context.writeByte(SELECTOR_GAS_STATION);
		else if (equals(StorageTypes.STORAGE_TREE_ARRAY))
			context.writeByte(SELECTOR_STORAGE_TREE_ARRAY);
		else if (equals(StorageTypes.STORAGE_TREE_ARRAY_NODE))
			context.writeByte(SELECTOR_STORAGE_TREE_ARRAY_NODE);
		else if (equals(StorageTypes.OBJECT))
			context.writeByte(SELECTOR_OBJECT);
		else if (equals(StorageTypes.CONTRACT))
			context.writeByte(SELECTOR_CONTRACT);
		else if (equals(StorageTypes.STORAGE))
			context.writeByte(SELECTOR_STORAGE);
		else if (equals(StorageTypes.PAYABLE_CONTRACT))
			context.writeByte(SELECTOR_PAYABLE_CONTRACT);
		else if (name.equals(Constants.STORAGE_MAP_VIEW_NAME))
			context.writeByte(SELECTOR_STORAGE_MAP_VIEW);
		else if (equals(StorageTypes.STORAGE_TREE_MAP))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP);
		else if (equals(StorageTypes.STORAGE_TREE_MAP_BLACK_NODE))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_BLACK_NODE);
		else if (equals(StorageTypes.STORAGE_TREE_MAP_RED_NODE))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_RED_NODE);
		else if (equals(StorageTypes.STORAGE_TREE_INTMAP_NODE))
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE);
		else if (equals(StorageTypes.STORAGE_TREE_SET))
			context.writeByte(SELECTOR_STORAGE_TREE_SET);
		else if (name.equals(Constants.STORAGE_LIST_VIEW_NAME))
			context.writeByte(SELECTOR_STORAGE_LIST);
		else if (name.equals(Constants.STORAGE_TREE_MAP_NODE_NAME))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE);
		else if (name.equals(Constants.STORAGE_LINKED_LIST_NODE_NAME))
			context.writeByte(SELECTOR_STORAGE_LINKED_LIST_NODE);
		else if (equals(StorageTypes.PAYABLE_CONTRACT))
			context.writeByte(SELECTOR_PAYABLE_CONTRACT);
		else if (equals(StorageTypes.EOA))
			context.writeByte(SELECTOR_EOA);
		else if (equals(StorageTypes.GENERIC_GAS_STATION))
			context.writeByte(SELECTOR_GENERIC_GAS_STATION);
		else if (equals(StorageTypes.EVENT))
			context.writeByte(SELECTOR_EVENT);
		else if (name.startsWith(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME)) {
			context.writeByte(SELECTOR_IO_TAKAMAKA_CODE_LANG);
			// we drop the initial io.takamaka.code.lang. portion of the name
			context.writeStringShared(name.substring(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME.length()));
		}
		else if (name.startsWith(Constants.IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME)) {
			context.writeByte(SELECTOR_IO_TAKAMAKA_CODE_UTIL);
			// we drop the initial io.takamaka.code.util. portion of the name
			context.writeStringShared(name.substring(Constants.IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME.length()));
		}
		else if (name.startsWith(Constants.IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME)) {
			context.writeByte(SELECTOR_IO_TAKAMAKA_CODE_TOKENS);
			// we drop the initial io.takamaka.code.tokens. portion of the name
			context.writeStringShared(name.substring(Constants.IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME.length()));
		}
		else if (name.startsWith(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME)) {
			context.writeByte(SELECTOR_IO_TAKAMAKA_CODE);
			// we drop the initial io.takamaka.code. portion of the name
			context.writeStringShared(name.substring(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME.length()));
		}
		else {
			context.writeByte(SELECTOR); // to distinguish from the basic types
			context.writeStringShared(name);
		}
	}

	@Override
	public boolean isEager() {
		return equals(StorageTypes.BIG_INTEGER) || equals(StorageTypes.STRING);
	}
}
