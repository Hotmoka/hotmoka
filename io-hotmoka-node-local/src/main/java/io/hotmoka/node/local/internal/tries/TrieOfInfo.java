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

package io.hotmoka.node.local.internal.tries;

import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.patricia.api.UnknownKeyException;

/**
 * A Merkle-Patricia trie that maps miscellaneous information into their value.
 */
public class TrieOfInfo extends AbstractPatriciaTrie<Byte, StorageValue, TrieOfInfo> {

	/**
	 * Builds a Merkle-Patricia trie that maps miscellaneous information into their value.
	 * It uses sha256 as hashing algorithm for the trie's nodes and an array of 0's to represent
	 * the empty trie.
	 * 
	 * @param store the supporting key/value store
	 * @param root the root of the trie to check out
	 * @throws UnknownKeyException if {@code root} cannot be found in the trie
	 */
	public TrieOfInfo(KeyValueStore store, byte[] root) throws UnknownKeyException {
		super(store, root, HashingAlgorithms.identity1().getHasher(key -> new byte[] { key }),
			// we use a NodeUnmarshallingContext because that is the default used for marshalling storage values
			mkSHA256(), new byte[32], StorageValue::toByteArray, bytes -> StorageValues.from(NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))));
	}

	private TrieOfInfo(TrieOfInfo cloned, byte[] root) throws UnknownKeyException {
		super(cloned, root);
	}

	private static HashingAlgorithm mkSHA256() {
		try {
			return HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new LocalNodeException(e);
		}
	}

	@Override
	protected void malloc() {
		super.malloc();
	}

	@Override
	protected void free() {
		super.free();
	}

	@Override
	public TrieOfInfo checkoutAt(byte[] root) throws UnknownKeyException {
		return new TrieOfInfo(this, root);
	}

	/**
	 * Yields the manifest.
	 * 
	 * @return the manifest, if any
	 */
	public Optional<StorageReference> getManifest() {
		return get((byte) 0)
			.map(value -> value.asReference(value2 -> new TrieException("This trie contains a manifest that is not a StorageReference but rather a " + value2.getClass().getName())));
	}

	/**
	 * Sets the manifest.
	 * 
	 * @param manifest the manifest to set
	 */
	public TrieOfInfo setManifest(StorageReference manifest) {
		return put((byte) 0, manifest);
	}
}