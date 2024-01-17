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

package io.hotmoka.beans.internal.types;

import java.io.IOException;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.constants.Constants;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A class type that can be used for stored objects in blockchain.
 */
@Immutable
public final class ClassTypeImpl extends AbstractStorageType implements ClassType {

	/**
	 * The frequently used class type for {@link java.lang.Object}.
	 */
	public final static ClassType OBJECT = new ClassTypeImpl(Object.class.getName());

	/**
	 * The frequently used class type for {@link java.lang.String}.
	 */
	public final static ClassType STRING = new ClassTypeImpl(String.class.getName());

	/**
	 * The frequently used class type for {@link java.math.BigInteger}.
	 */
	public final static ClassType BIG_INTEGER = new ClassTypeImpl("java.math.BigInteger");

	/**
	 * The frequently used class type for {@link io.takamaka.code.math.UnsignedBigInteger}.
	 */
	public final static ClassType UNSIGNED_BIG_INTEGER = new ClassTypeImpl(Constants.UNSIGNED_BIG_INTEGER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.ERC20}.
	 */
	public final static ClassType ERC20 = new ClassTypeImpl(Constants.ERC20_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasPriceUpdate}.
	 */
	public final static ClassType GAS_PRICE_UPDATE = new ClassTypeImpl(Constants.GAS_PRICE_UPDATE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = new ClassTypeImpl(Constants.EOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountED25519}.
	 */
	public final static ClassType EOA_ED25519 = new ClassTypeImpl(Constants.EOA_ED25519_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountEHA256DSA}.
	 */
	public final static ClassType EOA_SHA256DSA = new ClassTypeImpl(Constants.EOA_SHA256DSA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1}.
	 */
	public final static ClassType EOA_QTESLA1 = new ClassTypeImpl(Constants.EOA_QTESLA1_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3}.
	 */
	public final static ClassType EOA_QTESLA3 = new ClassTypeImpl(Constants.EOA_QTESLA3_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static ClassType CONTRACT = new ClassTypeImpl(Constants.CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Gamete}.
	 */
	public final static ClassType GAMETE = new ClassTypeImpl(Constants.GAMETE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Account}.
	 */
	public final static ClassType ACCOUNT = new ClassTypeImpl(Constants.ACCOUNT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Accounts}.
	 */
	public final static ClassType ACCOUNTS = new ClassTypeImpl(Constants.ACCOUNTS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.IERC20}.
	 */
	public final static ClassType IERC20 = new ClassTypeImpl(Constants.IERC20_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Manifest}.
	 */
	public final static ClassType MANIFEST = new ClassTypeImpl(Constants.MANIFEST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validator}.
	 */
	public final static ClassType VALIDATOR = new ClassTypeImpl(Constants.VALIDATOR_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validators}.
	 */
	public final static ClassType VALIDATORS = new ClassTypeImpl(Constants.VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.AbstractValidators}.
	 */
	public final static ClassType ABSTRACT_VALIDATORS = new ClassTypeImpl(Constants.ABSTRACT_VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Versions}.
	 */
	public final static ClassType VERSIONS = new ClassTypeImpl(Constants.VERSIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.AccountsLedger}.
	 */
	public final static ClassType ACCOUNTS_LEDGER = new ClassTypeImpl(Constants.ACCOUNTS_LEDGER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasStation}.
	 */
	public final static ClassType GAS_STATION = new ClassTypeImpl(Constants.GAS_STATION_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static ClassType GENERIC_GAS_STATION = new ClassTypeImpl(Constants.GENERIC_GAS_STATION_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintValidators}.
	 */
	public final static ClassType TENDERMINT_VALIDATORS = new ClassTypeImpl(Constants.TENDERMINT_VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintED25519Validator}.
	 */
	public final static ClassType TENDERMINT_ED25519_VALIDATOR = new ClassTypeImpl(Constants.TENDERMINT_ED25519_VALIDATOR_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static ClassType STORAGE = new ClassTypeImpl(Constants.STORAGE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static ClassType TAKAMAKA = new ClassTypeImpl(Constants.TAKAMAKA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static ClassType EVENT = new ClassTypeImpl(Constants.EVENT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static ClassType PAYABLE_CONTRACT = new ClassTypeImpl(Constants.PAYABLE_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	public final static ClassType FROM_CONTRACT = new ClassTypeImpl(Constants.FROM_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static ClassType VIEW = new ClassTypeImpl(Constants.VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static ClassType PAYABLE = new ClassTypeImpl(Constants.PAYABLE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static ClassType THROWS_EXCEPTIONS = new ClassTypeImpl(Constants.THROWS_EXCEPTIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32}.
	 */
	public final static ClassType BYTES32 = new ClassTypeImpl("io.takamaka.code.util.Bytes32");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32Snapshot}.
	 */
	public final static ClassType BYTES32_SNAPSHOT = new ClassTypeImpl("io.takamaka.code.util.Bytes32Snapshot");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageArray}.
	 */
	public final static ClassType STORAGE_ARRAY = new ClassTypeImpl(Constants.STORAGE_ARRAY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageListView}.
	 */
	public final static ClassType STORAGE_LIST_VIEW = new ClassTypeImpl(Constants.STORAGE_LIST_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static ClassType STORAGE_LINKED_LIST = new ClassTypeImpl(Constants.STORAGE_LINKED_LIST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMapView}.
	 */
	public final static ClassType STORAGE_MAP_VIEW = new ClassTypeImpl(Constants.STORAGE_MAP_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static ClassType STORAGE_TREE_MAP = new ClassTypeImpl(Constants.STORAGE_TREE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY = new ClassTypeImpl(Constants.STORAGE_TREE_ARRAY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray.Node}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY_NODE = new ClassTypeImpl(Constants.STORAGE_TREE_ARRAY_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP = new ClassTypeImpl(Constants.STORAGE_TREE_INTMAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeSet}.
	 */
	public final static ClassType STORAGE_TREE_SET = new ClassTypeImpl(Constants.STORAGE_TREE_SET_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.BlackNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_BLACK_NODE = new ClassTypeImpl(Constants.STORAGE_TREE_MAP_BLACK_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.RedNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_RED_NODE = new ClassTypeImpl(Constants.STORAGE_TREE_MAP_RED_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageSetView}.
	 */
	public final static ClassType STORAGE_SET_VIEW = new ClassTypeImpl(Constants.STORAGE_SET_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static ClassType STORAGE_MAP = new ClassTypeImpl(Constants.STORAGE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	public final static ClassType STORAGE_LINKED_LIST_NODE = new ClassTypeImpl(Constants.STORAGE_LINKED_LIST_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_MAP_NODE = new ClassTypeImpl(Constants.STORAGE_TREE_MAP_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP_NODE = new ClassTypeImpl(Constants.STORAGE_TREE_INTMAP_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericValidators}.
	 */
	public final static ClassType GENERIC_VALIDATORS = new ClassTypeImpl(Constants.GENERIC_VALIDATORS_NAME);
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.Poll}.
	 */
	public final static ClassType POLL = new ClassTypeImpl(Constants.POLL_NAME);
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity}.
	 */
	public static final ClassType SHARED_ENTITY =  new ClassTypeImpl(Constants.SHARED_ENTITY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity.Offer}.
	 */
	public static final ClassType SHARED_ENTITY_OFFER =  new ClassTypeImpl(Constants.SHARED_ENTITY_OFFER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntityView}.
	 */
	public static final ClassType SHARED_ENTITY_VIEW =  new ClassTypeImpl(Constants.SHARED_ENTITY_VIEW_NAME);
	
	private final static byte SELECTOR = 8;  // we start at 8 to distinguish the class types from the basic types
	private final static byte SELECTOR_IO_TAKAMAKA_CODE = 9;
	private final static byte SELECTOR_IO_TAKAMAKA_CODE_LANG = 10;
	private final static byte SELECTOR_IO_TAKAMAKA_CODE_UTIL = 11;
	private final static byte SELECTOR_IO_TAKAMAKA_CODE_TOKENS = 34;
	private final static byte SELECTOR_STORAGE_LIST = 12;
	private final static byte SELECTOR_STORAGE_TREE_MAP_NODE = 13;
	private final static byte SELECTOR_STORAGE_LINKED_LIST_NODE = 14;
	private final static byte SELECTOR_EOA = 15;
	private final static byte SELECTOR_GAS_PRICE_UPDATE = 16;
	private final static byte SELECTOR_STRING = 17;
	private final static byte SELECTOR_ACCOUNT = 18;
	private final static byte SELECTOR_MANIFEST = 19;
	private final static byte SELECTOR_CONTRACT = 20;
	private final static byte SELECTOR_OBJECT = 22;
	private final static byte SELECTOR_STORAGE = 23;
	private final static byte SELECTOR_GENERIC_GAS_STATION = 24;
	private final static byte SELECTOR_EVENT = 25;
	private final static byte SELECTOR_BIGINTEGER = 26;
	private final static byte SELECTOR_PAYABLE_CONTRACT = 27;
	private final static byte SELECTOR_STORAGE_MAP_VIEW = 28;
	private final static byte SELECTOR_STORAGE_TREE_MAP = 29;
	private final static byte SELECTOR_STORAGE_TREE_MAP_BLACK_NODE = 30;
	private final static byte SELECTOR_STORAGE_TREE_MAP_RED_NODE = 31;
	private final static byte SELECTOR_UNSIGNED_BIG_INTEGER = 32;
	private final static byte SELECTOR_ERC20 = 33;
	private final static byte SELECTOR_IERC20 = 35;
	private final static byte SELECTOR_STORAGE_TREE_ARRAY = 36;
	private final static byte SELECTOR_STORAGE_TREE_ARRAY_NODE = 37;
	private final static byte SELECTOR_STORAGE_TREE_INTMAP_NODE = 38;
	private final static byte SELECTOR_STORAGE_TREE_SET = 39;
	private final static byte SELECTOR_GAS_STATION = 40;

	/**
	 * The name of the class type.
	 */
	private final String name;

	/**
	 * Builds a class type that can be used for storage objects in blockchain.
	 * 
	 * @param name the name of the class
	 */
	private ClassTypeImpl(String name) {
		this.name = Objects.requireNonNull(name, "name cannot be null");
	}

	/**
	 * Yields the class type for a class with the given name.
	 * 
	 * @param className the name of the class
	 * @return the class type
	 */
	public static ClassType named(String className) {
		switch (className) {
		case "java.math.BigInteger": return BIG_INTEGER;
		case "java.lang.Object": return OBJECT;
		case "java.lang.String": return STRING;
		case Constants.UNSIGNED_BIG_INTEGER_NAME: return UNSIGNED_BIG_INTEGER;
		case Constants.ERC20_NAME: return ERC20;
		case Constants.GAS_PRICE_UPDATE_NAME: return GAS_PRICE_UPDATE;
		case Constants.EOA_NAME: return EOA;
		case Constants.EOA_ED25519_NAME: return EOA_ED25519;
		case Constants.EOA_SHA256DSA_NAME: return EOA_SHA256DSA;
		case Constants.EOA_QTESLA1_NAME: return EOA_QTESLA1;
		case Constants.EOA_QTESLA3_NAME: return EOA_QTESLA3;
		case Constants.CONTRACT_NAME: return CONTRACT;
		case Constants.GAMETE_NAME: return GAMETE;
		case Constants.ACCOUNT_NAME: return ACCOUNT;
		case Constants.ACCOUNTS_NAME: return ACCOUNTS;
		case Constants.IERC20_NAME: return IERC20;
		case Constants.MANIFEST_NAME: return MANIFEST;
		case Constants.VALIDATOR_NAME: return VALIDATOR;
		case Constants.VALIDATORS_NAME: return VALIDATORS;
		case Constants.ABSTRACT_VALIDATORS_NAME: return ABSTRACT_VALIDATORS;
		case Constants.VERSIONS_NAME: return VERSIONS;
		case Constants.ACCOUNTS_LEDGER_NAME: return ACCOUNTS_LEDGER;
		case Constants.GAS_STATION_NAME: return GAS_STATION;
		case Constants.GENERIC_GAS_STATION_NAME: return GENERIC_GAS_STATION;
		case Constants.TENDERMINT_VALIDATORS_NAME: return TENDERMINT_VALIDATORS;
		case Constants.TENDERMINT_ED25519_VALIDATOR_NAME: return TENDERMINT_ED25519_VALIDATOR;
		case Constants.STORAGE_NAME: return STORAGE;
		case Constants.TAKAMAKA_NAME: return TAKAMAKA;
		case Constants.EVENT_NAME: return EVENT;
		case Constants.PAYABLE_CONTRACT_NAME: return PAYABLE_CONTRACT;
		case Constants.FROM_CONTRACT_NAME: return FROM_CONTRACT;
		case Constants.VIEW_NAME: return VIEW;
		case Constants.PAYABLE_NAME: return PAYABLE;
		case Constants.THROWS_EXCEPTIONS_NAME: return THROWS_EXCEPTIONS;
		case "io.takamaka.code.util.Bytes32": return BYTES32;
		case "io.takamaka.code.util.Bytes32Snapshot": return BYTES32_SNAPSHOT;
		case Constants.STORAGE_ARRAY_NAME: return STORAGE_ARRAY;
		case Constants.STORAGE_LIST_VIEW_NAME: return STORAGE_LIST_VIEW;
		case Constants.STORAGE_LINKED_LIST_NAME: return STORAGE_LINKED_LIST;
		case Constants.STORAGE_MAP_VIEW_NAME: return STORAGE_MAP_VIEW;
		case Constants.STORAGE_TREE_MAP_NAME: return STORAGE_TREE_MAP;
		case Constants.STORAGE_TREE_ARRAY_NAME: return STORAGE_TREE_ARRAY;
		case Constants.STORAGE_TREE_ARRAY_NODE_NAME: return STORAGE_TREE_ARRAY_NODE;
		case Constants.STORAGE_TREE_INTMAP_NAME: return STORAGE_TREE_INTMAP;
		case Constants.STORAGE_TREE_SET_NAME: return STORAGE_TREE_SET;
		case Constants.STORAGE_TREE_MAP_BLACK_NODE_NAME: return STORAGE_TREE_MAP_BLACK_NODE;
		case Constants.STORAGE_TREE_MAP_RED_NODE_NAME: return STORAGE_TREE_MAP_RED_NODE;
		case Constants.STORAGE_SET_VIEW_NAME: return STORAGE_SET_VIEW;
		case Constants.STORAGE_MAP_NAME: return STORAGE_MAP;
		case Constants.STORAGE_LINKED_LIST_NODE_NAME: return STORAGE_LINKED_LIST_NODE;
		case Constants.STORAGE_TREE_MAP_NODE_NAME: return STORAGE_TREE_MAP_NODE;
		case Constants.STORAGE_TREE_INTMAP_NODE_NAME: return STORAGE_TREE_INTMAP_NODE;
		case Constants.GENERIC_VALIDATORS_NAME: return GENERIC_VALIDATORS;
		case Constants.POLL_NAME: return POLL;
		case Constants.SHARED_ENTITY_NAME: return SHARED_ENTITY;
		case Constants.SHARED_ENTITY_OFFER_NAME: return SHARED_ENTITY_OFFER;
		default: return new ClassTypeImpl(className);
		}
	}

	/**
	 * Yields the class type with the given selector, unmarshalled from the given context.
	 * 
	 * @param selector the selector, already unmarshalled from the context
	 * @param context the unmarshalling context
	 * @return the class type, if any; this is {@code null} if the selector is illegal
     */
	static ClassType withSelector(byte selector, UnmarshallingContext context) throws IOException {
		switch (selector) {
		case SELECTOR:
			return named(context.readStringShared());
		case SELECTOR_BIGINTEGER:
			return BIG_INTEGER;
		case SELECTOR_ERC20:
			return ERC20;
		case SELECTOR_IERC20:
			return IERC20;
		case SELECTOR_STRING:
			return STRING;
		case SELECTOR_ACCOUNT:
			return ACCOUNT;
		case SELECTOR_CONTRACT:
			return CONTRACT;
		case SELECTOR_OBJECT:
			return OBJECT;
		case SELECTOR_STORAGE:
			return STORAGE;
		case SELECTOR_MANIFEST:
			return MANIFEST;
		case SELECTOR_GAS_STATION:
			return GAS_STATION;
		case SELECTOR_PAYABLE_CONTRACT:
			return PAYABLE_CONTRACT;
		case SELECTOR_STORAGE_LIST:
			return STORAGE_LIST_VIEW;
		case SELECTOR_STORAGE_MAP_VIEW:
			return STORAGE_MAP_VIEW;
		case SELECTOR_STORAGE_TREE_ARRAY:
			return STORAGE_TREE_ARRAY;
		case SELECTOR_STORAGE_TREE_ARRAY_NODE:
			return STORAGE_TREE_ARRAY_NODE;
		case SELECTOR_STORAGE_TREE_INTMAP_NODE:
			return STORAGE_TREE_INTMAP_NODE;
		case SELECTOR_STORAGE_TREE_SET:
			return STORAGE_TREE_SET;
		case SELECTOR_STORAGE_TREE_MAP:
			return STORAGE_TREE_MAP;
		case SELECTOR_STORAGE_TREE_MAP_BLACK_NODE:
			return STORAGE_TREE_MAP_BLACK_NODE;
		case SELECTOR_STORAGE_TREE_MAP_RED_NODE:
			return STORAGE_TREE_MAP_RED_NODE;
		case SELECTOR_STORAGE_LINKED_LIST_NODE:
			return STORAGE_LINKED_LIST_NODE;
		case SELECTOR_STORAGE_TREE_MAP_NODE:
			return STORAGE_TREE_MAP_NODE;
		case SELECTOR_EOA:
			return EOA;
		case SELECTOR_UNSIGNED_BIG_INTEGER:
			return UNSIGNED_BIG_INTEGER;
		case SELECTOR_GAS_PRICE_UPDATE:
			return GAS_PRICE_UPDATE;
		case SELECTOR_GENERIC_GAS_STATION:
			return GENERIC_GAS_STATION;
		case SELECTOR_EVENT:
			return EVENT;
		case SELECTOR_IO_TAKAMAKA_CODE:
			return named(Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME + context.readStringShared());
		case ClassTypeImpl.SELECTOR_IO_TAKAMAKA_CODE_LANG:
			return named(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME + context.readStringShared());
		case ClassTypeImpl.SELECTOR_IO_TAKAMAKA_CODE_UTIL:
			return named(Constants.IO_TAKAMAKA_CODE_UTIL_PACKAGE_NAME + context.readStringShared());
		case ClassTypeImpl.SELECTOR_IO_TAKAMAKA_CODE_TOKENS:
			return named(Constants.IO_TAKAMAKA_CODE_TOKENS_PACKAGE_NAME + context.readStringShared());
		default:
			return null;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassTypeImpl ct && ct.name.equals(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public int compareTo(StorageType other) {
		if (other instanceof ClassType ct)
			return name.compareTo(ct.getName());
		else // other instanceof BasicTypeImpl
			return 1;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		if (equals(BIG_INTEGER))
			context.writeByte(SELECTOR_BIGINTEGER);
		else if (equals(UNSIGNED_BIG_INTEGER))
			context.writeByte(SELECTOR_UNSIGNED_BIG_INTEGER);
		else if (equals(GAS_PRICE_UPDATE))
			context.writeByte(SELECTOR_GAS_PRICE_UPDATE);
		else if (equals(ERC20))
			context.writeByte(SELECTOR_ERC20);
		else if (equals(IERC20))
			context.writeByte(SELECTOR_IERC20);
		else if (equals(STRING))
			context.writeByte(SELECTOR_STRING);
		else if (equals(ACCOUNT))
			context.writeByte(SELECTOR_ACCOUNT);
		else if (equals(MANIFEST))
			context.writeByte(SELECTOR_MANIFEST);
		else if (equals(GAS_STATION))
			context.writeByte(SELECTOR_GAS_STATION);
		else if (equals(STORAGE_TREE_ARRAY))
			context.writeByte(SELECTOR_STORAGE_TREE_ARRAY);
		else if (equals(STORAGE_TREE_ARRAY_NODE))
			context.writeByte(SELECTOR_STORAGE_TREE_ARRAY_NODE);
		else if (equals(OBJECT))
			context.writeByte(SELECTOR_OBJECT);
		else if (equals(CONTRACT))
			context.writeByte(SELECTOR_CONTRACT);
		else if (equals(STORAGE))
			context.writeByte(SELECTOR_STORAGE);
		else if (equals(PAYABLE_CONTRACT))
			context.writeByte(SELECTOR_PAYABLE_CONTRACT);
		else if (equals(STORAGE_MAP_VIEW))
			context.writeByte(SELECTOR_STORAGE_MAP_VIEW);
		else if (equals(STORAGE_TREE_MAP))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP);
		else if (equals(STORAGE_TREE_MAP_BLACK_NODE))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_BLACK_NODE);
		else if (equals(STORAGE_TREE_MAP_RED_NODE))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_RED_NODE);
		else if (equals(STORAGE_TREE_INTMAP_NODE))
			context.writeByte(SELECTOR_STORAGE_TREE_INTMAP_NODE);
		else if (equals(STORAGE_TREE_SET))
			context.writeByte(SELECTOR_STORAGE_TREE_SET);
		else if (equals(STORAGE_LIST_VIEW))
			context.writeByte(SELECTOR_STORAGE_LIST);
		else if (name.equals(Constants.STORAGE_TREE_MAP_NODE_NAME))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE);
		else if (name.equals(Constants.STORAGE_LINKED_LIST_NODE_NAME))
			context.writeByte(SELECTOR_STORAGE_LINKED_LIST_NODE);
		else if (equals(PAYABLE_CONTRACT))
			context.writeByte(SELECTOR_PAYABLE_CONTRACT);
		else if (equals(EOA))
			context.writeByte(SELECTOR_EOA);
		else if (equals(GENERIC_GAS_STATION))
			context.writeByte(SELECTOR_GENERIC_GAS_STATION);
		else if (equals(EVENT))
			context.writeByte(SELECTOR_EVENT);
		else if (name.startsWith(Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME)) {
			context.writeByte(SELECTOR_IO_TAKAMAKA_CODE_LANG);
			// we drop the initial io.takamaka.code.lang. portion of the package name
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
			context.writeByte(SELECTOR);
			context.writeStringShared(name);
		}
	}

	@Override
	public boolean isEager() {
		return equals(BIG_INTEGER) || equals(STRING);
	}
}