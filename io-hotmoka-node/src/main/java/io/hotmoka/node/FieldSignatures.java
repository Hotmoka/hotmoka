/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node;

import java.io.IOException;

import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.gson.FieldSignatureDecoder;
import io.hotmoka.node.internal.gson.FieldSignatureEncoder;
import io.hotmoka.node.internal.gson.FieldSignatureJson;
import io.hotmoka.node.internal.signatures.FieldSignatureImpl;

/**
 * Providers of field signatures.
 */
public abstract class FieldSignatures {

	private FieldSignatures() {}

	/**
	 * Yields the signature of a field.
	 * 
	 * @param definingClass the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 * @return the field signature
	 */
	public static FieldSignature of(ClassType definingClass, String name, StorageType type) {
		return new FieldSignatureImpl(definingClass, name, type);
	}

	/**
	 * Yields the signature of a field.
	 * 
	 * @param definingClass the name of the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 * @return the field signature
	 */
	public static FieldSignature of(String definingClass, String name, StorageType type) {
		return new FieldSignatureImpl(StorageTypes.classNamed(definingClass), name, type);
	}

	/**
	 * Unmarshals a signature of a field from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the field signature
	 * @throws IOException if the field signature could not be unmarshalled
	 */
	public static FieldSignature from(UnmarshallingContext context) throws IOException {
		return FieldSignatureImpl.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends FieldSignatureEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends FieldSignatureDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends FieldSignatureJson {

    	/**
    	 * Creates the Json representation for the given field signature.
    	 * 
    	 * @param field the field signature
    	 */
    	public Json(FieldSignature field) {
    		super(field);
    	}
    }

    /**
	 * The field that holds the balance in contracts.
	 */
	public final static FieldSignature BALANCE_FIELD = FieldSignatureImpl.BALANCE_FIELD;

	/**
	 * The field that holds the nonce in externally owned accounts.
	 */
	public final static FieldSignature EOA_NONCE_FIELD = FieldSignatureImpl.EOA_NONCE_FIELD;

	/**
	 * The field that holds the public key in externally owned accounts.
	 */
	public final static FieldSignature EOA_PUBLIC_KEY_FIELD = FieldSignatureImpl.EOA_PUBLIC_KEY_FIELD;

	/**
	 * The field of the manifest that holds the contract of the validators of the node.
	 */
	public final static FieldSignature MANIFEST_VALIDATORS_FIELD = FieldSignatureImpl.MANIFEST_VALIDATORS_FIELD;

	/**
	 * The field of the manifest that holds the object that keeps track
	 * of the versions of the modules of the node.
	 */
	public final static FieldSignature MANIFEST_VERSIONS_FIELD = FieldSignatureImpl.MANIFEST_VERSIONS_FIELD;

	/**
	 * The field of the manifest that holds the gas station.
	 */
	public final static FieldSignature MANIFEST_GAS_STATION_FIELD = FieldSignatureImpl.MANIFEST_GAS_STATION_FIELD;

	/**
	 * The field of the manifest that holds the gamete account of the node.
	 */
	public final static FieldSignature MANIFEST_GAMETE_FIELD = FieldSignatureImpl.MANIFEST_GAMETE_FIELD;

	/**
	 * The field that holds the creator of an event.
	 */
	public final static FieldSignature EVENT_CREATOR_FIELD = FieldSignatureImpl.EVENT_CREATOR_FIELD;

	/**
	 * The field that holds the gas price inside a {@code io.takamaka.code.governance.GenericGasStation}.
	 */
	public final static FieldSignature GENERIC_GAS_STATION_GAS_PRICE_FIELD = FieldSignatureImpl.GENERIC_GAS_STATION_GAS_PRICE_FIELD;

	/**
	 * The field that holds the current supply inside a {@code io.takamaka.code.governance.AbstractValidators}.
	 */
	public final static FieldSignature ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD = FieldSignatureImpl.ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD;

	/**
	 * The field that holds the gas price inside a {@code io.takamaka.code.math.UnsignedBigInteger.value}.
	 */
	public final static FieldSignature UNSIGNED_BIG_INTEGER_VALUE_FIELD = FieldSignatureImpl.UNSIGNED_BIG_INTEGER_VALUE_FIELD;

	/**
	 * The field that holds the root of a {@code io.takamaka.code.util.StorageTreeMap}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_ROOT_FIELD = FieldSignatureImpl.STORAGE_TREE_MAP_ROOT_FIELD;

	/**
	 * The field that holds the root of a {@code io.takamaka.code.util.StorageIntTreeMap}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_ROOT_FIELD = FieldSignatureImpl.STORAGE_TREE_INTMAP_ROOT_FIELD;

	/**
	 * The field that holds the size of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_SIZE_FIELD = FieldSignatureImpl.STORAGE_TREE_MAP_NODE_SIZE_FIELD;

	/**
	 * The field that holds the size of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_SIZE_FIELD = FieldSignatureImpl.STORAGE_TREE_INTMAP_NODE_SIZE_FIELD;

	/**
	 * The field that holds the value of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_VALUE_FIELD = FieldSignatureImpl.STORAGE_TREE_INTMAP_NODE_VALUE_FIELD;

	/**
	 * The field that holds the left child of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_LEFT_FIELD = FieldSignatureImpl.STORAGE_TREE_INTMAP_NODE_LEFT_FIELD;

	/**
	 * The field that holds the right child of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD = FieldSignatureImpl.STORAGE_TREE_INTMAP_NODE_RIGHT_FIELD;

	/**
	 * The field that holds the key of a {@code io.takamaka.code.util.StorageTreeIntMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_INTMAP_NODE_KEY_FIELD = FieldSignatureImpl.STORAGE_TREE_INTMAP_NODE_KEY_FIELD;

	/**
	 * The field that holds the left tree of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_LEFT_FIELD = FieldSignatureImpl.STORAGE_TREE_MAP_NODE_LEFT_FIELD;

	/**
	 * The field that holds the right tree of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_RIGHT_FIELD = FieldSignatureImpl.STORAGE_TREE_MAP_NODE_RIGHT_FIELD;

	/**
	 * The field that holds the key of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_KEY_FIELD = FieldSignatureImpl.STORAGE_TREE_MAP_NODE_KEY_FIELD;

	/**
	 * The field that holds the value of a {@code io.takamaka.code.util.StorageTreeMap.Node}.
	 */
	public final static FieldSignature STORAGE_TREE_MAP_NODE_VALUE_FIELD = FieldSignatureImpl.STORAGE_TREE_MAP_NODE_VALUE_FIELD;
}