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

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.patricia.PatriciaTries;
import io.hotmoka.patricia.api.PatriciaTrie;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A Merkle-Patricia trie that maps miscellaneous information into their value.
 */
public class TrieOfInfo {

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<Byte, StorageValue> parent;

	/**
	 * Builds a Merkle-Patricia trie that maps miscellaneous information into their value.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data must be garbage collected (eventually)
	 *                        as result of the store updates performed during that commit; you can pass
	 *                        -1L if the trie is used only for reading
	 */
	public TrieOfInfo(Store store, Transaction txn, byte[] root, long numberOfCommits) {
		try {
			var keyValueStoreOfInfos = new KeyValueStoreOnXodus(store, txn, root);
			parent = PatriciaTries.of(keyValueStoreOfInfos, HashingAlgorithms.identity1().getHasher(key -> new byte[] { key }),
				HashingAlgorithms.sha256(), StorageValues::from, NodeUnmarshallingContexts::of, numberOfCommits);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception", e);
		}
	}

	/**
	 * Yields the root of the trie, that can be used as a hash of the trie itself.
	 * 
	 * @return the root
	 */
	public byte[] getRoot() {
		return parent.getRoot();
	}

	/**
	 * Yields the number of commits.
	 * 
	 * @return the number of commits. This is 0 if the number of commits has not been set yet
	 */
	public long getNumberOfCommits() {
		return parent.get((byte) 0)
			.map(commits -> ((LongValue) commits).getValue())
			.orElse(0L);
	}

	/**
	 * Increases the number of commits.
	 * 
	 * @return the new (ie, incremented) number of commits
	 */
	public long increaseNumberOfCommits() {
		long numberOfCommits = getNumberOfCommits() + 1;
		parent.put((byte) 0, StorageValues.longOf(numberOfCommits));
		return numberOfCommits;
	}

	/**
	 * Yields the manifest.
	 * 
	 * @return the manifest, if any
	 */
	public Optional<StorageReference> getManifest() {
		return parent.get((byte) 1)
			.map(manifest -> (StorageReference) manifest);
	}

	/**
	 * Sets the manifest.
	 * 
	 * @param manifest the manifest to set
	 */
	public void setManifest(StorageReference manifest) {
		parent.put((byte) 1, manifest);
	}

	/**
	 * Garbage-collects all keys that have been updated during the given number of commit.
	 * 
	 * @param commitNumber the number of the commit to garbage collect
	 */
	public void garbageCollect(long commitNumber) {
		parent.garbageCollect(commitNumber);
	}
}