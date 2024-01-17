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

import io.hotmoka.beans.api.types.BasicType;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.internal.gson.StorageTypeDecoder;
import io.hotmoka.beans.internal.gson.StorageTypeEncoder;
import io.hotmoka.beans.internal.gson.StorageTypeJson;
import io.hotmoka.beans.internal.types.AbstractStorageType;
import io.hotmoka.beans.internal.types.BasicTypeImpl;
import io.hotmoka.beans.internal.types.ClassTypeImpl;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of storage types.
 */
public abstract class StorageTypes {

	private StorageTypes() {}

	/**
	 * Yields the storage type with the given name.
	 * 
	 * @param name the name of the type
	 * @return the storage type
	 */
	public static StorageType named(String name) {
		return AbstractStorageType.named(name);
	}

	/**
	 * Yields the class type for a class with the given name.
	 * 
	 * @param className the name of the class
	 * @return the class type
	 */
	public static ClassType classNamed(String className) {
		return ClassTypeImpl.named(className);
	}

	/**
	 * Yields the storage type corresponding to the given class.
	 * 
	 * @param clazz the class
	 * @return the storage type
	 */
	public static StorageType fromClass(Class<?> clazz) {
		return AbstractStorageType.fromClass(clazz);
	}

	/**
	 * Yields the storage type unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the storage type
	 * @throws IOException if the type cannot be marshalled
     */
	public static StorageType from(UnmarshallingContext context) throws IOException {
		return AbstractStorageType.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends StorageTypeEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends StorageTypeDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends StorageTypeJson {

    	/**
    	 * Creates the Json representation for the given type.
    	 * 
    	 * @param type the type
    	 */
    	public Json(StorageType type) {
    		super(type);
    	}
    }

	/**
	 * The {@code boolean} basic type of the Takamaka language.
	 */
	public final static BasicType BOOLEAN = BasicTypeImpl.BOOLEAN;

	/**
	 * The {@code byte} basic type of the Takamaka language.
	 */
	public final static BasicType BYTE = BasicTypeImpl.BYTE;

	/**
	 * The {@code char} basic type of the Takamaka language.
	 */
	public final static BasicType CHAR = BasicTypeImpl.CHAR;

	/**
	 * The {@code short} basic type of the Takamaka language.
	 */
	public final static BasicType SHORT = BasicTypeImpl.SHORT;

	/**
	 * The {@code int} basic type of the Takamaka language.
	 */
	public final static BasicType INT = BasicTypeImpl.INT;

	/**
	 * The {@code long} basic type of the Takamaka language.
	 */
	public final static BasicType LONG = BasicTypeImpl.LONG;

	/**
	 * The {@code float} basic type of the Takamaka language.
	 */
	public final static BasicType FLOAT = BasicTypeImpl.FLOAT;
	
	/**
	 * The {@code double} basic type of the Takamaka language.
	 */
	public final static BasicType DOUBLE = BasicTypeImpl.DOUBLE;

	/**
	 * The frequently used class type for {@link java.lang.Object}.
	 */
	public final static ClassType OBJECT = ClassTypeImpl.OBJECT;

	/**
	 * The frequently used class type for {@link java.lang.String}.
	 */
	public final static ClassType STRING = ClassTypeImpl.STRING;

	/**
	 * The frequently used class type for {@link java.math.BigInteger}.
	 */
	public final static ClassType BIG_INTEGER = ClassTypeImpl.BIG_INTEGER;

	/**
	 * The frequently used class type for {@link io.takamaka.code.math.UnsignedBigInteger}.
	 */
	public final static ClassType UNSIGNED_BIG_INTEGER = ClassTypeImpl.UNSIGNED_BIG_INTEGER;

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.ERC20}.
	 */
	public final static ClassType ERC20 = ClassTypeImpl.ERC20;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasPriceUpdate}.
	 */
	public final static ClassType GAS_PRICE_UPDATE = ClassTypeImpl.GAS_PRICE_UPDATE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = ClassTypeImpl.EOA;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountED25519}.
	 */
	public final static ClassType EOA_ED25519 = ClassTypeImpl.EOA_ED25519;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountEHA256DSA}.
	 */
	public final static ClassType EOA_SHA256DSA = ClassTypeImpl.EOA_SHA256DSA;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1}.
	 */
	public final static ClassType EOA_QTESLA1 = ClassTypeImpl.EOA_QTESLA1;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3}.
	 */
	public final static ClassType EOA_QTESLA3 = ClassTypeImpl.EOA_QTESLA3;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static ClassType CONTRACT = ClassTypeImpl.CONTRACT;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Gamete}.
	 */
	public final static ClassType GAMETE = ClassTypeImpl.GAMETE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Account}.
	 */
	public final static ClassType ACCOUNT = ClassTypeImpl.ACCOUNT;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Accounts}.
	 */
	public final static ClassType ACCOUNTS = ClassTypeImpl.ACCOUNTS;

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.IERC20}.
	 */
	public final static ClassType IERC20 = ClassTypeImpl.IERC20;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Manifest}.
	 */
	public final static ClassType MANIFEST = ClassTypeImpl.MANIFEST;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validator}.
	 */
	public final static ClassType VALIDATOR = ClassTypeImpl.VALIDATOR;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validators}.
	 */
	public final static ClassType VALIDATORS = ClassTypeImpl.VALIDATORS;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.AbstractValidators}.
	 */
	public final static ClassType ABSTRACT_VALIDATORS = ClassTypeImpl.ABSTRACT_VALIDATORS;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Versions}.
	 */
	public final static ClassType VERSIONS = ClassTypeImpl.VERSIONS;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.AccountsLedger}.
	 */
	public final static ClassType ACCOUNTS_LEDGER = ClassTypeImpl.ACCOUNTS_LEDGER;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasStation}.
	 */
	public final static ClassType GAS_STATION = ClassTypeImpl.GAS_STATION;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static ClassType GENERIC_GAS_STATION = ClassTypeImpl.GENERIC_GAS_STATION;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintValidators}.
	 */
	public final static ClassType TENDERMINT_VALIDATORS = ClassTypeImpl.TENDERMINT_VALIDATORS;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintED25519Validator}.
	 */
	public final static ClassType TENDERMINT_ED25519_VALIDATOR = ClassTypeImpl.TENDERMINT_ED25519_VALIDATOR;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static ClassType STORAGE = ClassTypeImpl.STORAGE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static ClassType TAKAMAKA = ClassTypeImpl.TAKAMAKA;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static ClassType EVENT = ClassTypeImpl.EVENT;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static ClassType PAYABLE_CONTRACT = ClassTypeImpl.PAYABLE_CONTRACT;

	/**
	 * The frequently used class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	public final static ClassType FROM_CONTRACT = ClassTypeImpl.FROM_CONTRACT;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static ClassType VIEW = ClassTypeImpl.VIEW;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static ClassType PAYABLE = ClassTypeImpl.PAYABLE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static ClassType THROWS_EXCEPTIONS = ClassTypeImpl.THROWS_EXCEPTIONS;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32}.
	 */
	public final static ClassType BYTES32 = ClassTypeImpl.BYTES32;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32Snapshot}.
	 */
	public final static ClassType BYTES32_SNAPSHOT = ClassTypeImpl.BYTES32_SNAPSHOT;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageArray}.
	 */
	public final static ClassType STORAGE_ARRAY = ClassTypeImpl.STORAGE_ARRAY;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageListView}.
	 */
	public final static ClassType STORAGE_LIST_VIEW = ClassTypeImpl.STORAGE_LIST_VIEW;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static ClassType STORAGE_LINKED_LIST = ClassTypeImpl.STORAGE_LINKED_LIST;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMapView}.
	 */
	public final static ClassType STORAGE_MAP_VIEW = ClassTypeImpl.STORAGE_MAP_VIEW;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static ClassType STORAGE_TREE_MAP = ClassTypeImpl.STORAGE_TREE_MAP;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY = ClassTypeImpl.STORAGE_TREE_ARRAY;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray.Node}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY_NODE = ClassTypeImpl.STORAGE_TREE_ARRAY_NODE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP = ClassTypeImpl.STORAGE_TREE_INTMAP;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeSet}.
	 */
	public final static ClassType STORAGE_TREE_SET = ClassTypeImpl.STORAGE_TREE_SET;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.BlackNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_BLACK_NODE = ClassTypeImpl.STORAGE_TREE_MAP_BLACK_NODE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.RedNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_RED_NODE = ClassTypeImpl.STORAGE_TREE_MAP_RED_NODE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageSetView}.
	 */
	public final static ClassType STORAGE_SET_VIEW = ClassTypeImpl.STORAGE_SET_VIEW;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static ClassType STORAGE_MAP = ClassTypeImpl.STORAGE_MAP;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	public final static ClassType STORAGE_LINKED_LIST_NODE = ClassTypeImpl.STORAGE_LINKED_LIST_NODE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_MAP_NODE = ClassTypeImpl.STORAGE_TREE_MAP_NODE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP_NODE = ClassTypeImpl.STORAGE_TREE_INTMAP_NODE;

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericValidators}.
	 */
	public final static ClassType GENERIC_VALIDATORS = ClassTypeImpl.GENERIC_VALIDATORS;
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.Poll}.
	 */
	public final static ClassType POLL = ClassTypeImpl.POLL;
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity}.
	 */
	public static final ClassType SHARED_ENTITY = ClassTypeImpl.SHARED_ENTITY;

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity.Offer}.
	 */
	public static final ClassType SHARED_ENTITY_OFFER =  ClassTypeImpl.SHARED_ENTITY_OFFER;

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntityView}.
	 */
	public static final ClassType SHARED_ENTITY_VIEW =  ClassTypeImpl.SHARED_ENTITY_VIEW;
}