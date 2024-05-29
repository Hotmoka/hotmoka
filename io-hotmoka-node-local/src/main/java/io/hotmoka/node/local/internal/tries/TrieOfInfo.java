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
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.TrieException;

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
	 * @param root the root of the trie to check out; use empty to create the empty trie
	 */
	public TrieOfInfo(KeyValueStore store, byte[] root) throws TrieException {
		super(store, root, HashingAlgorithms.identity1().getHasher(key -> new byte[] { key }),
			sha256(), new byte[32], StorageValue::toByteArray, bytes -> StorageValues.from(NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))));
	}

	private TrieOfInfo(TrieOfInfo cloned, byte[] root) throws TrieException {
		super(cloned, root);
	}

	@Override
	public TrieOfInfo checkoutAt(byte[] root) throws TrieException {
		return new TrieOfInfo(this, root);
	}

	/**
	 * Yields the block height.
	 * 
	 * @return the block height of this store
	 * @throws TrieException if the operation cannot be completed correctly
	 */
	public long getHeight() throws TrieException {
		return get((byte) 0)
			.map(height -> ((LongValue) height).getValue())
			.orElse(0L);
	}

	/**
	 * Increases the block height.
	 * 
	 * @return a trie identical to this but with an incremented block height
	 * @throws TrieException if the operation cannot be completed correctly
	 */
	public TrieOfInfo increaseHeight() throws TrieException {
		return put((byte) 0, StorageValues.longOf(getHeight() + 1));
	}

	/**
	 * Yields the manifest.
	 * 
	 * @return the manifest, if any
	 */
	public Optional<StorageReference> getManifest() throws TrieException {
		Optional<StorageValue> maybeManifest = get((byte) 1);
		if (maybeManifest.isEmpty())
			return Optional.empty();
		else if (maybeManifest.get() instanceof StorageReference manifest)
			return Optional.of(manifest);
		else
			throw new TrieException("This trie contains a manifest that is not a StorageReference");
	}

	/**
	 * Sets the manifest.
	 * 
	 * @param manifest the manifest to set
	 * @throws TrieException if this trie is not able to complete the operation correcly
	 */
	public TrieOfInfo setManifest(StorageReference manifest) throws TrieException {
		return put((byte) 1, manifest);
	}

	private static HashingAlgorithm sha256() throws TrieException {
		try {
			return HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new TrieException(e);		}
	}
}