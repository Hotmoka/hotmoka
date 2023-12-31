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

package io.hotmoka.beans;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.api.types.BasicType;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.types.internal.BasicTypeImpl;
import io.hotmoka.beans.types.internal.ClassTypeImpl;
import io.hotmoka.constants.Constants;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of storage types.
 */
public abstract class StorageTypes {

	private StorageTypes() {}

	/**
	 * The {@code boolean} basic type of the Takamaka language.
	 */
	public final static BasicType BOOLEAN = new BasicTypeImpl(BasicTypeImpl.Instance.BOOLEAN);

	/**
	 * The {@code byte} basic type of the Takamaka language.
	 */
	public final static BasicType BYTE = new BasicTypeImpl(BasicTypeImpl.Instance.BYTE);

	/**
	 * The {@code char} basic type of the Takamaka language.
	 */
	public final static BasicType CHAR = new BasicTypeImpl(BasicTypeImpl.Instance.CHAR);

	/**
	 * The {@code short} basic type of the Takamaka language.
	 */
	public final static BasicType SHORT = new BasicTypeImpl(BasicTypeImpl.Instance.SHORT);

	/**
	 * The {@code int} basic type of the Takamaka language.
	 */
	public final static BasicType INT = new BasicTypeImpl(BasicTypeImpl.Instance.INT);

	/**
	 * The {@code long} basic type of the Takamaka language.
	 */
	public final static BasicType LONG = new BasicTypeImpl(BasicTypeImpl.Instance.LONG);

	/**
	 * The {@code float} basic type of the Takamaka language.
	 */
	public final static BasicType FLOAT = new BasicTypeImpl(BasicTypeImpl.Instance.FLOAT);
	
	/**
	 * The {@code double} basic type of the Takamaka language.
	 */
	public final static BasicType DOUBLE = new BasicTypeImpl(BasicTypeImpl.Instance.DOUBLE);

	/**
	 * The frequently used class type for {@link java.lang.Object}.
	 */
	public final static ClassType OBJECT = classNamed(Object.class.getName());

	/**
	 * The frequently used class type for {@link java.lang.String}.
	 */
	public final static ClassType STRING = classNamed(String.class.getName());

	/**
	 * The frequently used class type for {@link java.math.BigInteger}.
	 */
	public final static ClassType BIG_INTEGER = classNamed(BigInteger.class.getName());

	/**
	 * The frequently used class type for {@link io.takamaka.code.math.UnsignedBigInteger}.
	 */
	public final static ClassType UNSIGNED_BIG_INTEGER = classNamed(Constants.UNSIGNED_BIG_INTEGER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.ERC20}.
	 */
	public final static ClassType ERC20 = classNamed(Constants.ERC20_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasPriceUpdate}.
	 */
	public final static ClassType GAS_PRICE_UPDATE = classNamed(Constants.GAS_PRICE_UPDATE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = classNamed(Constants.EOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountED25519}.
	 */
	public final static ClassType EOA_ED25519 = classNamed(Constants.EOA_ED25519_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountEHA256DSA}.
	 */
	public final static ClassType EOA_SHA256DSA = classNamed(Constants.EOA_SHA256DSA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1}.
	 */
	public final static ClassType EOA_QTESLA1 = classNamed(Constants.EOA_QTESLA1_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3}.
	 */
	public final static ClassType EOA_QTESLA3 = classNamed(Constants.EOA_QTESLA3_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static ClassType CONTRACT = classNamed(Constants.CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Gamete}.
	 */
	public final static ClassType GAMETE = classNamed(Constants.GAMETE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Account}.
	 */
	public final static ClassType ACCOUNT = classNamed(Constants.ACCOUNT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Accounts}.
	 */
	public final static ClassType ACCOUNTS = classNamed(Constants.ACCOUNTS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.IERC20}.
	 */
	public final static ClassType IERC20 = classNamed(Constants.IERC20_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Manifest}.
	 */
	public final static ClassType MANIFEST = classNamed(Constants.MANIFEST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validator}.
	 */
	public final static ClassType VALIDATOR = classNamed(Constants.VALIDATOR_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validators}.
	 */
	public final static ClassType VALIDATORS = classNamed(Constants.VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.AbstractValidators}.
	 */
	public final static ClassType ABSTRACT_VALIDATORS = classNamed(Constants.ABSTRACT_VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Versions}.
	 */
	public final static ClassType VERSIONS = classNamed(Constants.VERSIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.AccountsLedger}.
	 */
	public final static ClassType ACCOUNTS_LEDGER = classNamed(Constants.ACCOUNTS_LEDGER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasStation}.
	 */
	public final static ClassType GAS_STATION = classNamed(Constants.GAS_STATION_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static ClassType GENERIC_GAS_STATION = classNamed(Constants.GENERIC_GAS_STATION_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintValidators}.
	 */
	public final static ClassType TENDERMINT_VALIDATORS = classNamed(Constants.TENDERMINT_VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintED25519Validator}.
	 */
	public final static ClassType TENDERMINT_ED25519_VALIDATOR = classNamed(Constants.TENDERMINT_ED25519_VALIDATOR_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static ClassType STORAGE = classNamed(Constants.STORAGE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static ClassType TAKAMAKA = classNamed(Constants.TAKAMAKA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static ClassType EVENT = classNamed(Constants.EVENT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static ClassType PAYABLE_CONTRACT = classNamed(Constants.PAYABLE_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	public final static ClassType FROM_CONTRACT = classNamed(Constants.FROM_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static ClassType VIEW = classNamed(Constants.VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static ClassType PAYABLE = classNamed(Constants.PAYABLE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static ClassType THROWS_EXCEPTIONS = classNamed(Constants.THROWS_EXCEPTIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32}.
	 */
	public final static ClassType BYTES32 = classNamed("io.takamaka.code.util.Bytes32");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32Snapshot}.
	 */
	public final static ClassType BYTES32_SNAPSHOT = classNamed("io.takamaka.code.util.Bytes32Snapshot");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageArray}.
	 */
	public final static ClassType STORAGE_ARRAY = classNamed(Constants.STORAGE_ARRAY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageListView}.
	 */
	public final static ClassType STORAGE_LIST = classNamed(Constants.STORAGE_LIST_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static ClassType STORAGE_LINKED_LIST = classNamed(Constants.STORAGE_LINKED_LIST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMapView}.
	 */
	public final static ClassType STORAGE_MAP_VIEW = classNamed(Constants.STORAGE_MAP_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static ClassType STORAGE_TREE_MAP = classNamed(Constants.STORAGE_TREE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY = classNamed(Constants.STORAGE_TREE_ARRAY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray.Node}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY_NODE = classNamed(Constants.STORAGE_TREE_ARRAY_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP = classNamed(Constants.STORAGE_TREE_INTMAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeSet}.
	 */
	public final static ClassType STORAGE_TREE_SET = classNamed(Constants.STORAGE_TREE_SET_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.BlackNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_BLACK_NODE = classNamed(Constants.STORAGE_TREE_MAP_BLACK_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.RedNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_RED_NODE = classNamed(Constants.STORAGE_TREE_MAP_RED_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageSetView}.
	 */
	public final static ClassType STORAGE_SET_VIEW = classNamed(Constants.STORAGE_SET_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static ClassType MODIFIABLE_STORAGE_MAP = classNamed(Constants.STORAGE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	public final static ClassType STORAGE_LINKED_LIST_NODE = classNamed(Constants.STORAGE_LINKED_LIST_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_MAP_NODE = classNamed(Constants.STORAGE_TREE_MAP_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP_NODE = classNamed(Constants.STORAGE_TREE_INTMAP_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericValidators}.
	 */
	public final static ClassType GENERIC_VALIDATORS = classNamed(Constants.GENERIC_VALIDATORS_NAME);
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.Poll}.
	 */
	public final static ClassType POLL = classNamed(Constants.POLL_NAME);
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity}.
	 */
	public static final ClassType SHARED_ENTITY =  classNamed(Constants.SHARED_ENTITY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity.Offer}.
	 */
	public static final ClassType SHARED_ENTITY_OFFER =  classNamed(Constants.SHARED_ENTITY_OFFER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntityView}.
	 */
	public static final ClassType SHARED_ENTITY_VIEW =  classNamed(Constants.SHARED_ENTITY_VIEW_NAME);

	/**
	 * Yields the storage type for a class with the given name.
	 * 
	 * @param className the name of the class
	 * @return the storage type
	 */
	public static ClassType classNamed(String className) {
		return new ClassTypeImpl(className);
	}

	/**
	 * Yields the storage type with the given name.
	 * 
	 * @param name the name of the type
	 * @return the storage type
	 */
	public static StorageType named(String name) {
    	switch (name) {
    	case "boolean":
            return StorageTypes.BOOLEAN;
        case "byte":
            return StorageTypes.BYTE;
        case "char":
            return StorageTypes.CHAR;
        case "short":
            return StorageTypes.SHORT;
        case "int":
            return StorageTypes.INT;
        case "long":
            return StorageTypes.LONG;
        case "float":
            return StorageTypes.FLOAT;
        case "double":
            return StorageTypes.DOUBLE;
        default:
        	return StorageTypes.classNamed(name);
    	}
	}

	/**
	 * Yields the storage type corresponding to the given class.
	 * 
	 * @param clazz the class
	 * @return the storage type
	 */
	public static StorageType of(Class<?> clazz) {
		if (clazz == boolean.class)
			return BOOLEAN;
		else if (clazz == byte.class)
			return BYTE;
		else if (clazz == char.class)
			return CHAR;
		else if (clazz == short.class)
			return SHORT;
		else if (clazz == int.class)
			return INT;
		else if (clazz == long.class)
			return LONG;
		else if (clazz == float.class)
			return FLOAT;
		else if (clazz == double.class)
			return DOUBLE;
		else
			return StorageTypes.classNamed(clazz.getName());
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
		case ClassTypeImpl.SELECTOR:
			return StorageTypes.classNamed(context.readStringShared());
		case ClassTypeImpl.SELECTOR_BIGINTEGER:
			return StorageTypes.BIG_INTEGER;
		case ClassTypeImpl.SELECTOR_ERC20:
			return StorageTypes.ERC20;
		case ClassTypeImpl.SELECTOR_IERC20:
			return StorageTypes.IERC20;
		case ClassTypeImpl.SELECTOR_STRING:
			return StorageTypes.STRING;
		case ClassTypeImpl.SELECTOR_ACCOUNT:
			return StorageTypes.ACCOUNT;
		case ClassTypeImpl.SELECTOR_CONTRACT:
			return StorageTypes.CONTRACT;
		case ClassTypeImpl.SELECTOR_OBJECT:
			return StorageTypes.OBJECT;
		case ClassTypeImpl.SELECTOR_STORAGE:
			return StorageTypes.STORAGE;
		case ClassTypeImpl.SELECTOR_MANIFEST:
			return StorageTypes.MANIFEST;
		case ClassTypeImpl.SELECTOR_GAS_STATION:
			return StorageTypes.GAS_STATION;
		case ClassTypeImpl.SELECTOR_PAYABLE_CONTRACT:
			return StorageTypes.PAYABLE_CONTRACT;
		case ClassTypeImpl.SELECTOR_STORAGE_LIST:
			return StorageTypes.STORAGE_LIST;
		case ClassTypeImpl.SELECTOR_STORAGE_MAP_VIEW:
			return StorageTypes.STORAGE_MAP_VIEW;
		case ClassTypeImpl.SELECTOR_STORAGE_TREE_ARRAY:
			return StorageTypes.STORAGE_TREE_ARRAY;
		case ClassTypeImpl.SELECTOR_STORAGE_TREE_ARRAY_NODE:
			return StorageTypes.STORAGE_TREE_ARRAY_NODE;
		case ClassTypeImpl.SELECTOR_STORAGE_TREE_INTMAP_NODE:
			return StorageTypes.STORAGE_TREE_INTMAP_NODE;
		case ClassTypeImpl.SELECTOR_STORAGE_TREE_SET:
			return StorageTypes.STORAGE_TREE_SET;
		case ClassTypeImpl.SELECTOR_STORAGE_TREE_MAP:
			return StorageTypes.STORAGE_TREE_MAP;
		case ClassTypeImpl.SELECTOR_STORAGE_TREE_MAP_BLACK_NODE:
			return StorageTypes.STORAGE_TREE_MAP_BLACK_NODE;
		case ClassTypeImpl.SELECTOR_STORAGE_TREE_MAP_RED_NODE:
			return StorageTypes.STORAGE_TREE_MAP_RED_NODE;
		case ClassTypeImpl.SELECTOR_STORAGE_LINKED_LIST_NODE:
			return StorageTypes.STORAGE_LINKED_LIST_NODE;
		case ClassTypeImpl.SELECTOR_STORAGE_TREE_MAP_NODE:
			return StorageTypes.STORAGE_TREE_MAP_NODE;
		case ClassTypeImpl.SELECTOR_EOA:
			return StorageTypes.EOA;
		case ClassTypeImpl.SELECTOR_UNSIGNED_BIG_INTEGER:
			return StorageTypes.UNSIGNED_BIG_INTEGER;
		case ClassTypeImpl.SELECTOR_GAS_PRICE_UPDATE:
			return StorageTypes.GAS_PRICE_UPDATE;
		case ClassTypeImpl.SELECTOR_GENERIC_GAS_STATION:
			return StorageTypes.GENERIC_GAS_STATION;
		case ClassTypeImpl.SELECTOR_EVENT:
			return StorageTypes.EVENT;
		case ClassTypeImpl.SELECTOR_IO_TAKAMAKA_CODE:
			return StorageTypes.classNamed(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME + context.readStringShared());
		case ClassTypeImpl.SELECTOR_IO_TAKAMAKA_CODE_LANG:
			return StorageTypes.classNamed(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + context.readStringShared());
		case ClassTypeImpl.SELECTOR_IO_TAKAMAKA_CODE_UTIL:
			return StorageTypes.classNamed(Constants.IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME + context.readStringShared());
		case ClassTypeImpl.SELECTOR_IO_TAKAMAKA_CODE_TOKENS:
			return StorageTypes.classNamed(Constants.IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME + context.readStringShared());
		case BasicTypeImpl.BOOLEAN_SELECTOR:
			return BOOLEAN;
		case BasicTypeImpl.BYTE_SELECTOR:
			return BYTE;
		case BasicTypeImpl.CHAR_SELECTOR:
			return CHAR;
		case BasicTypeImpl.SHORT_SELECTOR:
			return SHORT;
		case BasicTypeImpl.INT_SELECTOR:
			return INT;
		case BasicTypeImpl.LONG_SELECTOR:
			return LONG;
		case BasicTypeImpl.FLOAT_SELECTOR:
			return FLOAT;
		case BasicTypeImpl.DOUBLE_SELECTOR:
			return DOUBLE;
		default:
			throw new IOException("Unexpected type selector: " + selector);
		}
	}
}