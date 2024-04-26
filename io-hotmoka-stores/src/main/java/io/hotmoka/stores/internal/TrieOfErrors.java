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
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.patricia.PatriciaTries;
import io.hotmoka.patricia.api.PatriciaTrie;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A map from transaction requests into their error, backed by a Merkle-Patricia trie.
 */
public class TrieOfErrors {

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<TransactionReference, String> parent;

	/**
	 * Builds a Merkle-Patricia trie that maps transaction requests into their errors.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use empty to create the empty trie
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data must be garbage collected (eventually)
	 *                        as result of the store updates performed during that commit; you can pass
	 *                        -1L if the trie is used only for reading
	 */
	public TrieOfErrors(Store store, Transaction txn, Optional<byte[]> root, long numberOfCommits) {
		try {
			this.parent = PatriciaTries.of(new KeyValueStoreOnXodus(store, txn), root, HashingAlgorithms.identity32().getHasher(TransactionReference::getHash),
				HashingAlgorithms.sha256(), String::getBytes, String::new, numberOfCommits);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception", e);
		}
	}

	public Optional<String> get(TransactionReference key) throws TrieException {
		return parent.get(key);
	}

	public void put(TransactionReference key, String value) throws TrieException {
		parent.put(key, value);
	}

	public byte[] getRoot() throws TrieException {
		return parent.getRoot();
	}

	/**
	 * Garbage-collects all keys that have been updated during the given number of commit.
	 * 
	 * @param commitNumber the number of the commit to garbage collect
	 * @throws TrieException if this trie is not able to complete the operation correctly
	 */
	public void garbageCollect(long commitNumber) throws TrieException {
		parent.garbageCollect(commitNumber);
	}
}