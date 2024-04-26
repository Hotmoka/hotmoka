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

package io.hotmoka.stores.internal;

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
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A Merkle-Patricia trie that maps miscellaneous information into their value.
 */
public class TrieOfInfo extends AbstractPatriciaTrie<Byte, StorageValue, TrieOfInfo> {

	/**
	 * Builds a Merkle-Patricia trie that maps miscellaneous information into their value.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use empty to create the empty trie
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data must be garbage collected (eventually)
	 *                        as result of the store updates performed during that commit; you can pass
	 *                        -1L if the trie is used only for reading
	 */
	public TrieOfInfo(Store store, Transaction txn, Optional<byte[]> root, long numberOfCommits) {
		super(new KeyValueStoreOnXodus(store, txn), root, HashingAlgorithms.identity1().getHasher(key -> new byte[] { key }),
			sha256(), StorageValue::toByteArray, bytes -> StorageValues.from(NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))), numberOfCommits);
	}

	private TrieOfInfo(TrieOfInfo cloned, byte[] root) {
		super(cloned, root);
	}

	@Override
	protected TrieOfInfo cloneAndCheckout(byte[] root) {
		return new TrieOfInfo(this, root);
	}

	private static HashingAlgorithm sha256() {
		try {
			return HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	/**
	 * Yields the number of commits.
	 * 
	 * @return the number of commits. This is 0 if the number of commits has not been set yet
	 * @throws TrieException if the operation cannot be completed correctly
	 */
	public long getNumberOfCommits() throws TrieException {
		return get((byte) 0)
			.map(commits -> ((LongValue) commits).getValue())
			.orElse(0L);
	}

	/**
	 * Increases the number of commits.
	 * 
	 * @return the new (ie, incremented) number of commits
	 * @throws TrieException if the operation cannot be completed correctly
	 */
	public long increaseNumberOfCommits() throws TrieException {
		long numberOfCommits = getNumberOfCommits() + 1;
		put((byte) 0, StorageValues.longOf(numberOfCommits));
		return numberOfCommits;
	}

	/**
	 * Yields the manifest.
	 * 
	 * @return the manifest, if any
	 */
	public Optional<StorageReference> getManifest() {
		try {
			Optional<StorageValue> maybeManifest = get((byte) 1);
			if (maybeManifest.isEmpty())
				return Optional.empty();
			else if (maybeManifest.get() instanceof StorageReference manifest)
				return Optional.of(manifest);
			else
				throw new TrieException("This trie contains a manifest but is not a StorageReference");
		}
		catch (TrieException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	/**
	 * Sets the manifest.
	 * 
	 * @param manifest the manifest to set
	 * @throws TrieException if this trie is not able to complete the operatoin correcly
	 */
	public void setManifest(StorageReference manifest) throws TrieException {
		put((byte) 1, manifest);
	}
}