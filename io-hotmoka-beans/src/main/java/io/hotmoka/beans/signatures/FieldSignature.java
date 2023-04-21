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

package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.BeanMarshallable;
import io.hotmoka.beans.BeanMarshallingContext;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.constants.Constants;

/**
 * The signature of a field of a class.
 */
@Immutable
public final class FieldSignature extends BeanMarshallable implements Comparable<FieldSignature> {

	/**
	 * The field that holds the balance in contracts.
	 */
	public final static FieldSignature BALANCE_FIELD = new FieldSignature(ClassType.CONTRACT, "balance", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the red balance in contracts.
	 */
	public final static FieldSignature RED_BALANCE_FIELD = new FieldSignature(ClassType.CONTRACT, "balanceRed", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the nonce in externally owned accounts.
	 */
	public final static FieldSignature EOA_NONCE_FIELD = new FieldSignature(ClassType.EOA, "nonce", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the public key in externally owned accounts.
	 */
	public final static FieldSignature EOA_PUBLIC_KEY_FIELD = new FieldSignature(ClassType.EOA, "publicKey", ClassType.STRING);

	/**
	 * The field of the manifest that holds the contract of the validators of the node.
	 */
	public final static FieldSignature MANIFEST_VALIDATORS_FIELD = new FieldSignature(Constants.MANIFEST_NAME, "validators", ClassType.VALIDATORS);

	/**
	 * The field of the manifest that holds the object that keeps track
	 * of the versions of the modules of the node.
	 */
	public final static FieldSignature MANIFEST_VERSIONS_FIELD = new FieldSignature(Constants.MANIFEST_NAME, "versions", ClassType.VERSIONS);

	/**
	 * The field of the manifest that holds the gas station.
	 */
	public final static FieldSignature MANIFEST_GAS_STATION_FIELD = new FieldSignature(ClassType.MANIFEST, "gasStation", ClassType.GAS_STATION);

	/**
	 * The field of the manifest that holds the gamete account of the node.
	 */
	public final static FieldSignature MANIFEST_GAMETE_FIELD = new FieldSignature(Constants.MANIFEST_NAME, "gamete", ClassType.GAMETE);

	/**
	 * The field that holds the creator of an event.
	 */
	public final static FieldSignature EVENT_CREATOR_FIELD = new FieldSignature(ClassType.EVENT, "creator", ClassType.CONTRACT);

	/**
	 * The field that holds the gas price inside a {@code io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static FieldSignature GENERIC_GAS_STATION_GAS_PRICE_FIELD = new FieldSignature(ClassType.GENERIC_GAS_STATION, "gasPrice", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the current supply inside a {@code io.takamaka.code.governance.AbstractValidators}.
	 */
	public final static FieldSignature ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD = new FieldSignature(ClassType.ABSTRACT_VALIDATORS, "currentSupply", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the gas price inside a {@code io.takamaka.code.math.UnsignedBigInteger.value}.
	 */
	public final static FieldSignature UNSIGNED_BIG_INTEGER_VALUE_FIELD = new FieldSignature(ClassType.UNSIGNED_BIG_INTEGER, "value", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the root of a {@code io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_ROOT_FIELD = new FieldSignature(ClassType.STORAGE_TREE_MAP, "root", ClassType.STORAGE_TREE_MAP_NODE);

	/**
	 * The field that holds the root of a {@code io.takamaka.code.util.StorageIntTreeMap}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_ROOT_FIELD = new FieldSignature(ClassType.STORAGE_TREE_INTMAP, "root", ClassType.STORAGE_TREE_INTMAP_NODE);

	/**
	 * The field that holds the size of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_SIZE_FIELD = new FieldSignature(ClassType.STORAGE_TREE_MAP_NODE, "size", BasicTypes.INT);

	/**
	 * The field that holds the size of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_SIZE_FIELD = new FieldSignature(ClassType.STORAGE_TREE_INTMAP_NODE, "size", BasicTypes.INT);

	/**
	 * The field that holds the value of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_VALUE_FIELD = new FieldSignature(ClassType.STORAGE_TREE_INTMAP_NODE, "value", ClassType.OBJECT);

	/**
	 * The field that holds the left child of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_LEFT_FIELD = new FieldSignature(ClassType.STORAGE_TREE_INTMAP_NODE, "left", ClassType.STORAGE_TREE_INTMAP_NODE);

	/**
	 * The field that holds the right child of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD = new FieldSignature(ClassType.STORAGE_TREE_INTMAP_NODE, "right", ClassType.STORAGE_TREE_INTMAP_NODE);

	/**
	 * The field that holds the key of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_KEY_FIELD = new FieldSignature(ClassType.STORAGE_TREE_INTMAP_NODE, "key", BasicTypes.INT);

	/**
	 * The field that holds the left tree of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_LEFT_FIELD = new FieldSignature(ClassType.STORAGE_TREE_MAP_NODE, "left", ClassType.STORAGE_TREE_MAP_NODE);

	/**
	 * The field that holds the right tree of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_RIGHT_FIELD = new FieldSignature(ClassType.STORAGE_TREE_MAP_NODE, "right", ClassType.STORAGE_TREE_MAP_NODE);

	/**
	 * The field that holds the key of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_KEY_FIELD = new FieldSignature(ClassType.STORAGE_TREE_MAP_NODE, "key", ClassType.OBJECT);

	/**
	 * The field that holds the value of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_VALUE_FIELD = new FieldSignature(ClassType.STORAGE_TREE_MAP_NODE, "value", ClassType.OBJECT);

	/**
	 * The class of the field.
	 */
	public final ClassType definingClass;

	/**
	 * The name of the field.
	 */
	public final String name;

	/**
	 * The type of the field.
	 */
	public final StorageType type;

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public FieldSignature(ClassType definingClass, String name, StorageType type) {
		if (definingClass == null)
			throw new IllegalArgumentException("definingClass cannot be null");

		if (name == null)
			throw new IllegalArgumentException("name cannot be null");

		if (type == null)
			throw new IllegalArgumentException("type cannot be null");

		this.definingClass = definingClass;
		this.name = name;
		this.type = type;
	}

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the name of the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public FieldSignature(String definingClass, String name, StorageType type) {
		this(new ClassType(definingClass), name, type);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldSignature && ((FieldSignature) other).definingClass.equals(definingClass)
			&& ((FieldSignature) other).name.equals(name) && ((FieldSignature) other).type.equals(type);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ name.hashCode() ^ type.hashCode();
	}

	@Override
	public String toString() {
		return definingClass + "." + name + ":" + type;
	}

	@Override
	public int compareTo(FieldSignature other) {
		int diff = definingClass.compareAgainst(other.definingClass);
		if (diff != 0)
			return diff;

		diff = name.compareTo(other.name);
		return diff != 0 ? diff : type.compareAgainst(other.type);
	}

	/**
	 * Yields the size of this field, in terms of gas units consumed in store.
	 * 
	 * @param gasCostModel the model of the costs
	 * @return the size
	 */
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot()).add(definingClass.size(gasCostModel))
			.add(gasCostModel.storageCostOf(name)).add(type.size(gasCostModel));
	}

	@Override
	public void into(BeanMarshallingContext context) throws IOException {
		context.writeFieldSignature(this);
	}

	/**
	 * Factory method that unmarshals a field signature from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the field signature
	 * @throws IOException if the field signature could not be unmarshalled
	 * @throws ClassNotFoundException if the field signature could not be unmarshalled
	 */
	public static FieldSignature from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		return context.readFieldSignature();
	}

	@Override
	protected BeanMarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}