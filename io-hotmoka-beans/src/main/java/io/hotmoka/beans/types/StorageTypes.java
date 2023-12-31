/*
Copyright 2023 Fausto Spoto

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

import io.hotmoka.constants.Constants;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of storage types.
 */
public abstract class StorageTypes {

	private StorageTypes() {}

	/**
	 * The frequently used class type for {@link java.lang.Object}.
	 */
	public final static ClassType OBJECT = of(Object.class.getName());

	/**
	 * The frequently used class type for {@link java.lang.String}.
	 */
	public final static ClassType STRING = of(String.class.getName());

	/**
	 * The frequently used class type for {@link java.math.BigInteger}.
	 */
	public final static ClassType BIG_INTEGER = of(BigInteger.class.getName());

	/**
	 * The frequently used class type for {@link io.takamaka.code.math.UnsignedBigInteger}.
	 */
	public final static ClassType UNSIGNED_BIG_INTEGER = of(Constants.UNSIGNED_BIG_INTEGER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.ERC20}.
	 */
	public final static ClassType ERC20 = of(Constants.ERC20_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasPriceUpdate}.
	 */
	public final static ClassType GAS_PRICE_UPDATE = of(Constants.GAS_PRICE_UPDATE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = of(Constants.EOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountED25519}.
	 */
	public final static ClassType EOA_ED25519 = of(Constants.EOA_ED25519_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountEHA256DSA}.
	 */
	public final static ClassType EOA_SHA256DSA = of(Constants.EOA_SHA256DSA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1}.
	 */
	public final static ClassType EOA_QTESLA1 = of(Constants.EOA_QTESLA1_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3}.
	 */
	public final static ClassType EOA_QTESLA3 = of(Constants.EOA_QTESLA3_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static ClassType CONTRACT = of(Constants.CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Gamete}.
	 */
	public final static ClassType GAMETE = of(Constants.GAMETE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Account}.
	 */
	public final static ClassType ACCOUNT = of(Constants.ACCOUNT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Accounts}.
	 */
	public final static ClassType ACCOUNTS = of(Constants.ACCOUNTS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.IERC20}.
	 */
	public final static ClassType IERC20 = of(Constants.IERC20_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Manifest}.
	 */
	public final static ClassType MANIFEST = of(Constants.MANIFEST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validator}.
	 */
	public final static ClassType VALIDATOR = of(Constants.VALIDATOR_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validators}.
	 */
	public final static ClassType VALIDATORS = of(Constants.VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.AbstractValidators}.
	 */
	public final static ClassType ABSTRACT_VALIDATORS = of(Constants.ABSTRACT_VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Versions}.
	 */
	public final static ClassType VERSIONS = of(Constants.VERSIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.AccountsLedger}.
	 */
	public final static ClassType ACCOUNTS_LEDGER = of(Constants.ACCOUNTS_LEDGER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasStation}.
	 */
	public final static ClassType GAS_STATION = of(Constants.GAS_STATION_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static ClassType GENERIC_GAS_STATION = of(Constants.GENERIC_GAS_STATION_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintValidators}.
	 */
	public final static ClassType TENDERMINT_VALIDATORS = of(Constants.TENDERMINT_VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintED25519Validator}.
	 */
	public final static ClassType TENDERMINT_ED25519_VALIDATOR = of(Constants.TENDERMINT_ED25519_VALIDATOR_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static ClassType STORAGE = of(Constants.STORAGE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static ClassType TAKAMAKA = of(Constants.TAKAMAKA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static ClassType EVENT = of(Constants.EVENT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static ClassType PAYABLE_CONTRACT = of(Constants.PAYABLE_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	public final static ClassType FROM_CONTRACT = of(Constants.FROM_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static ClassType VIEW = of(Constants.VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static ClassType PAYABLE = of(Constants.PAYABLE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static ClassType THROWS_EXCEPTIONS = of(Constants.THROWS_EXCEPTIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32}.
	 */
	public final static ClassType BYTES32 = of("io.takamaka.code.util.Bytes32");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32Snapshot}.
	 */
	public final static ClassType BYTES32_SNAPSHOT = of("io.takamaka.code.util.Bytes32Snapshot");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageArray}.
	 */
	public final static ClassType STORAGE_ARRAY = of(Constants.STORAGE_ARRAY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageListView}.
	 */
	public final static ClassType STORAGE_LIST = of(Constants.STORAGE_LIST_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static ClassType STORAGE_LINKED_LIST = of(Constants.STORAGE_LINKED_LIST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMapView}.
	 */
	public final static ClassType STORAGE_MAP_VIEW = of(Constants.STORAGE_MAP_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static ClassType STORAGE_TREE_MAP = of(Constants.STORAGE_TREE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY = of(Constants.STORAGE_TREE_ARRAY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray.Node}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY_NODE = of(Constants.STORAGE_TREE_ARRAY_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP = of(Constants.STORAGE_TREE_INTMAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeSet}.
	 */
	public final static ClassType STORAGE_TREE_SET = of(Constants.STORAGE_TREE_SET_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.BlackNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_BLACK_NODE = of(Constants.STORAGE_TREE_MAP_BLACK_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.RedNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_RED_NODE = of(Constants.STORAGE_TREE_MAP_RED_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageSetView}.
	 */
	public final static ClassType STORAGE_SET_VIEW = of(Constants.STORAGE_SET_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static ClassType MODIFIABLE_STORAGE_MAP = of(Constants.STORAGE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	public final static ClassType STORAGE_LINKED_LIST_NODE = of(Constants.STORAGE_LINKED_LIST_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_MAP_NODE = of(Constants.STORAGE_TREE_MAP_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP_NODE = of(Constants.STORAGE_TREE_INTMAP_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericValidators}.
	 */
	public final static ClassType GENERIC_VALIDATORS = of(Constants.GENERIC_VALIDATORS_NAME);
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.Poll}.
	 */
	public final static ClassType POLL = of(Constants.POLL_NAME);
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity}.
	 */
	public static final ClassType SHARED_ENTITY =  of(Constants.SHARED_ENTITY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity.Offer}.
	 */
	public static final ClassType SHARED_ENTITY_OFFER =  of(Constants.SHARED_ENTITY_OFFER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntityView}.
	 */
	public static final ClassType SHARED_ENTITY_VIEW =  of(Constants.SHARED_ENTITY_VIEW_NAME);

	/**
	 * Yields the storage type for a class with the given name.
	 * 
	 * @param className the name of the class
	 * @return the storage type
	 */
	public static ClassType of(String className) {
		return new ClassType(className);
	}

	/**
	 * Yields the storage type corresponding to the given class.
	 * 
	 * @param clazz the class
	 * @return the storage type
	 */
	public static StorageType of(Class<?> clazz) {
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
			return StorageTypes.of(clazz.getName());
	}

	/**
	 * Yields the storage type unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the storage type
	 * @throws IOException if the type cannot be marshalled
     */
	public static StorageType from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();
		switch (selector) {
		case ClassType.SELECTOR:
			return StorageTypes.of(context.readStringShared());
		case ClassType.SELECTOR_BIGINTEGER:
			return StorageTypes.BIG_INTEGER;
		case ClassType.SELECTOR_ERC20:
			return StorageTypes.ERC20;
		case ClassType.SELECTOR_IERC20:
			return StorageTypes.IERC20;
		case ClassType.SELECTOR_STRING:
			return StorageTypes.STRING;
		case ClassType.SELECTOR_ACCOUNT:
			return StorageTypes.ACCOUNT;
		case ClassType.SELECTOR_CONTRACT:
			return StorageTypes.CONTRACT;
		case ClassType.SELECTOR_OBJECT:
			return StorageTypes.OBJECT;
		case ClassType.SELECTOR_STORAGE:
			return StorageTypes.STORAGE;
		case ClassType.SELECTOR_MANIFEST:
			return StorageTypes.MANIFEST;
		case ClassType.SELECTOR_GAS_STATION:
			return StorageTypes.GAS_STATION;
		case ClassType.SELECTOR_PAYABLE_CONTRACT:
			return StorageTypes.PAYABLE_CONTRACT;
		case ClassType.SELECTOR_STORAGE_LIST:
			return StorageTypes.STORAGE_LIST;
		case ClassType.SELECTOR_STORAGE_MAP_VIEW:
			return StorageTypes.STORAGE_MAP_VIEW;
		case ClassType.SELECTOR_STORAGE_TREE_ARRAY:
			return StorageTypes.STORAGE_TREE_ARRAY;
		case ClassType.SELECTOR_STORAGE_TREE_ARRAY_NODE:
			return StorageTypes.STORAGE_TREE_ARRAY_NODE;
		case ClassType.SELECTOR_STORAGE_TREE_INTMAP_NODE:
			return StorageTypes.STORAGE_TREE_INTMAP_NODE;
		case ClassType.SELECTOR_STORAGE_TREE_SET:
			return StorageTypes.STORAGE_TREE_SET;
		case ClassType.SELECTOR_STORAGE_TREE_MAP:
			return StorageTypes.STORAGE_TREE_MAP;
		case ClassType.SELECTOR_STORAGE_TREE_MAP_BLACK_NODE:
			return StorageTypes.STORAGE_TREE_MAP_BLACK_NODE;
		case ClassType.SELECTOR_STORAGE_TREE_MAP_RED_NODE:
			return StorageTypes.STORAGE_TREE_MAP_RED_NODE;
		case ClassType.SELECTOR_STORAGE_LINKED_LIST_NODE:
			return StorageTypes.STORAGE_LINKED_LIST_NODE;
		case ClassType.SELECTOR_STORAGE_TREE_MAP_NODE:
			return StorageTypes.STORAGE_TREE_MAP_NODE;
		case ClassType.SELECTOR_EOA:
			return StorageTypes.EOA;
		case ClassType.SELECTOR_UNSIGNED_BIG_INTEGER:
			return StorageTypes.UNSIGNED_BIG_INTEGER;
		case ClassType.SELECTOR_GAS_PRICE_UPDATE:
			return StorageTypes.GAS_PRICE_UPDATE;
		case ClassType.SELECTOR_GENERIC_GAS_STATION:
			return StorageTypes.GENERIC_GAS_STATION;
		case ClassType.SELECTOR_EVENT:
			return StorageTypes.EVENT;
		case ClassType.SELECTOR_IO_TAKAMAKA_CODE:
			return StorageTypes.of(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME + context.readStringShared());
		case ClassType.SELECTOR_IO_TAKAMAKA_CODE_LANG:
			return StorageTypes.of(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + context.readStringShared());
		case ClassType.SELECTOR_IO_TAKAMAKA_CODE_UTIL:
			return StorageTypes.of(Constants.IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME + context.readStringShared());
		case ClassType.SELECTOR_IO_TAKAMAKA_CODE_TOKENS:
			return StorageTypes.of(Constants.IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME + context.readStringShared());
		default:
			if (selector >= 0 && selector < 8)
				return BasicTypes.values()[selector];
			else
				throw new IOException("Unexpected type selector: " + selector);
		}
	}
}