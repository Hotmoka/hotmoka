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
	final static byte SELECTOR_BIGINTEGER = 9;
	final static byte SELECTOR_PAYABLE_CONTRACT = 10;
	final static byte SELECTOR_STORAGE_MAP = 11;
	final static byte SELECTOR_STORAGE_LIST = 12;
	final static byte SELECTOR_STORAGE_TREE_MAP_NODE = 13;
	final static byte SELECTOR_STORAGE_LINKED_LIST_NODE = 14;
	final static byte SELECTOR_EOA = 15;
	final static byte SELECTOR_TEOA = 16;
	final static byte SELECTOR_STRING = 17;
	final static byte SELECTOR_ACCOUNT = 18;
	final static byte SELECTOR_MANIFEST = 19;
	final static byte SELECTOR_CONTRACT = 20;
	final static byte SELECTOR_RGEOA = 21;
	final static byte SELECTOR_OBJECT = 22;
	final static byte SELECTOR_STORAGE = 23;

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
	 * The frequently used class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static ClassType EOA = new ClassType(Constants.EOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.RedGreenExternallyOwnedAccount}.
	 */
	public final static ClassType RGEOA = new ClassType(Constants.RGEOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.TestExternallyOwnedAccount}.
	 */
	public final static ClassType TEOA = new ClassType(Constants.TEOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount}.
	 */
	public final static ClassType TRGEOA = new ClassType(Constants.TRGEOA_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static ClassType CONTRACT = new ClassType(Constants.CONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.RedGreenContract}.
	 */
	public final static ClassType RGCONTRACT = new ClassType(Constants.RGCONTRACT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.lang.Account}.
	 */
	public final static ClassType ACCOUNT = new ClassType(Constants.ACCOUNT_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.system.Manifest}.
	 */
	public final static ClassType MANIFEST = new ClassType(Constants.MANIFEST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.system.Validator}.
	 */
	public final static ClassType VALIDATOR = new ClassType(Constants.VALIDATOR_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.system.Validators}.
	 */
	public final static ClassType VALIDATORS = new ClassType(Constants.VALIDATORS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.system.Versions}.
	 */
	public final static ClassType VERSIONS = new ClassType(Constants.VERSIONS_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.system.tendermint.TendermintValidators}.
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
	 * The frequently used class type for {@link io.takamaka.code.lang.RedGreenPayableContract}.
	 */
	public final static ClassType RGPAYABLE_CONTRACT = new ClassType(Constants.RGPAYABLE_CONTRACT_NAME);

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
	 * The frequently used class type for {@link io.takamaka.code.util.StorageList}.
	 */
	public final static ClassType STORAGE_LIST = new ClassType(Constants.STORAGE_LIST_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageLinkedList}.
	 */
	public final static ClassType STORAGE_LINKED_LIST = new ClassType(Constants.STORAGE_LINKED_LIST_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.StorageMap}.
	 */
	public final static ClassType STORAGE_MAP = new ClassType(Constants.STORAGE_MAP_VIEW_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.ModifiableStorageMap}.
	 */
	public final static ClassType MODIFIABLE_STORAGE_MAP = new ClassType(Constants.STORAGE_MAP_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.internal.StorageLinkedList.Node}.
	 */
	public final static ClassType STORAGE_LINKED_LIST_NODE = new ClassType(Constants.STORAGE_LINKED_LIST_NODE_NAME);

	/**
	 * The frequently used class type for {@link io.takamaka.code.util.internal.StorageTreeMap.Node}.
	 */
	public final static ClassType STORAGE_TREE_MAP_NODE = new ClassType(Constants.STORAGE_TREE_MAP_NODE_NAME);

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
		if (name.equals(BigInteger.class.getName()))
			context.oos.writeByte(SELECTOR_BIGINTEGER);
		else if (equals(STRING))
			context.oos.writeByte(SELECTOR_STRING);
		else if (equals(ACCOUNT))
			context.oos.writeByte(SELECTOR_ACCOUNT);
		else if (name.equals(Constants.MANIFEST_NAME))
			context.oos.writeByte(SELECTOR_MANIFEST);
		else if (equals(RGEOA))
			context.oos.writeByte(SELECTOR_RGEOA);
		else if (equals(OBJECT))
			context.oos.writeByte(SELECTOR_OBJECT);
		else if (equals(CONTRACT))
			context.oos.writeByte(SELECTOR_CONTRACT);
		else if (equals(STORAGE))
			context.oos.writeByte(SELECTOR_STORAGE);
		else if (name.equals(Constants.PAYABLE_CONTRACT_NAME))
			context.oos.writeByte(SELECTOR_PAYABLE_CONTRACT);
		else if (name.equals(Constants.STORAGE_MAP_VIEW_NAME))
			context.oos.writeByte(SELECTOR_STORAGE_MAP);
		else if (name.equals(Constants.STORAGE_LIST_VIEW_NAME))
			context.oos.writeByte(SELECTOR_STORAGE_LIST);
		else if (name.equals(Constants.STORAGE_TREE_MAP_NODE_NAME))
			context.oos.writeByte(SELECTOR_STORAGE_TREE_MAP_NODE);
		else if (name.equals(Constants.STORAGE_LINKED_LIST_NODE_NAME))
			context.oos.writeByte(SELECTOR_STORAGE_LINKED_LIST_NODE);
		else if (name.equals(Constants.PAYABLE_CONTRACT_NAME))
			context.oos.writeByte(SELECTOR_PAYABLE_CONTRACT);
		else if (name.equals(Constants.EOA_NAME))
			context.oos.writeByte(SELECTOR_EOA);
		else if (name.equals(Constants.TEOA_NAME))
			context.oos.writeByte(SELECTOR_TEOA);
		else {
			context.oos.writeByte(SELECTOR); // to distinguish from the basic types
			context.writeObject(name);
		}
	}

	@Override
	public boolean isEager() {
		return equals(BIG_INTEGER) || equals(STRING);
	}
}