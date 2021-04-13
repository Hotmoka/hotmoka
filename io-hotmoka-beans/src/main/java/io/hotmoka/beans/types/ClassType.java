package io.hotmoka.beans.types;

import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.takamaka.code.constants.Constants;

/**
 * A class type that can be used for stored objects in blockchain.
 */
@Immutable
public final class ClassType implements StorageType {
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
	final static byte SELECTOR_STORAGE_MAP = 28;
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
	 * The frequently used class type for {@link java.lang.Object}.
	 */
	public final static ClassType OBJECT = new ClassType(Object.class.getName());

	/**
	 * The frequently used class type for {@link java.lang.String}.
	 */
	public final static ClassType STRING = new ClassType(String.class.getName());

	/**
	 * The frequently used class type for {@link java.math.BigInteger}.
	 */
	public final static ClassType BIG_INTEGER = new ClassType(BigInteger.class.getName());

	/**
	 * The frequently used class type for {@link io.takamaka.code.math.UnsignedBigInteger}.
	 */
	public final static ClassType UNSIGNED_BIG_INTEGER = new ClassType(Constants.UNSIGNED_BIG_INTEGER_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.ERC20}.
	 */
	public final static ClassType ERC20 = new ClassType(Constants.ERC20_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasPriceUpdate}.
	 */
	public final static ClassType GAS_PRICE_UPDATE = new ClassType(Constants.GAS_PRICE_UPDATE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = new ClassType(Constants.EOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static ClassType CONTRACT = new ClassType(Constants.CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Gamete}.
	 */
	public final static ClassType GAMETE = new ClassType(Constants.GAMETE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Account}.
	 */
	public final static ClassType ACCOUNT = new ClassType(Constants.ACCOUNT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Accounts}.
	 */
	public final static ClassType ACCOUNTS = new ClassType(Constants.ACCOUNTS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.tokens.IERC20}.
	 */
	public final static ClassType IERC20 = new ClassType(Constants.IERC20_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Manifest}.
	 */
	public final static ClassType MANIFEST = new ClassType(Constants.MANIFEST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validator}.
	 */
	public final static ClassType VALIDATOR = new ClassType(Constants.VALIDATOR_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Validators}.
	 */
	public final static ClassType VALIDATORS = new ClassType(Constants.VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.Versions}.
	 */
	public final static ClassType VERSIONS = new ClassType(Constants.VERSIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GasStation}.
	 */
	public final static ClassType GAS_STATION = new ClassType(Constants.GAS_STATION_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static ClassType GENERIC_GAS_STATION = new ClassType(Constants.GENERIC_GAS_STATION_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.tendermint.TendermintValidators}.
	 */
	public final static ClassType TENDERMINT_VALIDATORS = new ClassType(Constants.TENDERMINT_VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static ClassType STORAGE = new ClassType(Constants.STORAGE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static ClassType TAKAMAKA = new ClassType(Constants.TAKAMAKA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static ClassType EVENT = new ClassType(Constants.EVENT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.PayableContract}.
	 */
	public final static ClassType PAYABLE_CONTRACT = new ClassType(Constants.PAYABLE_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@code io.takamaka.code.lang.FromContract}.
	 */
	public final static ClassType FROM_CONTRACT = new ClassType(Constants.FROM_CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static ClassType VIEW = new ClassType(Constants.VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static ClassType PAYABLE = new ClassType(Constants.PAYABLE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.ThrowsExceptions}.
	 */
	public final static ClassType THROWS_EXCEPTIONS = new ClassType(Constants.THROWS_EXCEPTIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32}.
	 */
	public final static ClassType BYTES32 = new ClassType("io.takamaka.code.util.Bytes32");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.Bytes32Snapshot}.
	 */
	public final static ClassType BYTES32_SNAPSHOT = new ClassType("io.takamaka.code.util.Bytes32Snapshot");

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageArray}.
	 */
	public final static ClassType STORAGE_ARRAY = new ClassType(Constants.STORAGE_ARRAY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageListView}.
	 */
	public final static ClassType STORAGE_LIST = new ClassType(Constants.STORAGE_LIST_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static ClassType STORAGE_LINKED_LIST = new ClassType(Constants.STORAGE_LINKED_LIST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMapView}.
	 */
	public final static ClassType STORAGE_MAP = new ClassType(Constants.STORAGE_MAP_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static ClassType STORAGE_TREE_MAP = new ClassType(Constants.STORAGE_TREE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY = new ClassType(Constants.STORAGE_TREE_ARRAY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeArray.Node}.
	 */
	public final static ClassType STORAGE_TREE_ARRAY_NODE = new ClassType(Constants.STORAGE_TREE_ARRAY_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP = new ClassType(Constants.STORAGE_TREE_INTMAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeSet}.
	 */
	public final static ClassType STORAGE_TREE_SET = new ClassType(Constants.STORAGE_TREE_SET_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.BlackNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_BLACK_NODE = new ClassType(Constants.STORAGE_TREE_MAP_BLACK_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.RedNode}.
	 */
	public final static ClassType STORAGE_TREE_MAP_RED_NODE = new ClassType(Constants.STORAGE_TREE_MAP_RED_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageSetView}.
	 */
	public final static ClassType STORAGE_SET_VIEW = new ClassType(Constants.STORAGE_SET_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static ClassType MODIFIABLE_STORAGE_MAP = new ClassType(Constants.STORAGE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList.Node}.
	 */
	public final static ClassType STORAGE_LINKED_LIST_NODE = new ClassType(Constants.STORAGE_LINKED_LIST_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_MAP_NODE = new ClassType(Constants.STORAGE_TREE_MAP_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_INTMAP_NODE = new ClassType(Constants.STORAGE_TREE_INTMAP_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.governance.GenericValidators}.
	 */
	public final static ClassType GENERIC_VALIDATORS = new ClassType(Constants.GENERIC_VALIDATORS_NAME);
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.Poll}.
	 */
	public final static ClassType POLL = new ClassType(Constants.POLL_NAME);
	
	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntity}.
	 */
	public static final ClassType SHARED_ENTITY =  new ClassType(Constants.SHARED_ENTITY_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.dao.SharedEntityView}.
	 */
	public static final ClassType SHARED_ENTITY_VIEW =  new ClassType(Constants.SHARED_ENTITY_VIEW_NAME);

	/**
	 * The name of the class type.
	 */
	public final String name;

	/**
	 * Builds a class type that can be used for storage objects in blockchain.
	 * 
	 * @param name the name of the class
	 */
	public ClassType(String name) {
		if (name == null)
			throw new IllegalArgumentException("name cannot be null");

		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ClassType && ((ClassType) other).name.equals(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public int compareAgainst(StorageType other) {
		if (other instanceof BasicTypes)
			return 1;
		else
			return name.compareTo(((ClassType) other).name); // other instanceof ClassType
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot()).add(gasCostModel.storageCostOf(name));
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
		else if (name.equals(Constants.STORAGE_MAP_VIEW_NAME))
			context.writeByte(SELECTOR_STORAGE_MAP);
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
		else if (name.equals(Constants.STORAGE_LIST_VIEW_NAME))
			context.writeByte(SELECTOR_STORAGE_LIST);
		else if (name.equals(Constants.STORAGE_TREE_MAP_NODE_NAME))
			context.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE);
		else if (name.equals(Constants.STORAGE_LINKED_LIST_NODE_NAME))
			context.writeByte(SELECTOR_STORAGE_LINKED_LIST_NODE);
		else if (name.equals(Constants.PAYABLE_CONTRACT_NAME))
			context.writeByte(SELECTOR_PAYABLE_CONTRACT);
		else if (equals(EOA))
			context.writeByte(SELECTOR_EOA);
		else if (equals(GENERIC_GAS_STATION))
			context.writeByte(SELECTOR_GENERIC_GAS_STATION);
		else if (equals(EVENT))
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
		return equals(BIG_INTEGER) || equals(STRING);
	}
}
