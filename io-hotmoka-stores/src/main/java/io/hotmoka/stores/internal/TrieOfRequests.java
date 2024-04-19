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

import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.patricia.PatriciaTries;
import io.hotmoka.patricia.api.PatriciaTrie;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A Merkle-Patricia trie that maps references to transaction requests into their request itself.
 */
public class TrieOfRequests implements PatriciaTrie<TransactionReference, TransactionRequest<?>> {

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<TransactionReference, TransactionRequest<?>> parent;

	/**
	 * Builds a Merkle-Patricia trie that maps references to transaction requests into their responses.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data must be garbage collected (eventually)
	 *                        as result of the store updates performed during that commit; you can pass
	 *                        -1L if the trie is used only for reading
	 */
	public TrieOfRequests(Store store, Transaction txn, byte[] root, long numberOfCommits) {
		try {
			var keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			parent = PatriciaTries.of(keyValueStoreOfResponses, HashingAlgorithms.identity32().getHasher(TransactionReference::getHash),
				HashingAlgorithms.sha256(), TransactionRequests::from, NodeUnmarshallingContexts::of, numberOfCommits);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception", e);
		}
	}

	@Override
	public Optional<TransactionRequest<?>> get(TransactionReference key) {
		return parent.get(key);
	}

	@Override
	public void put(TransactionReference key, TransactionRequest<?> value) {
		parent.put(key, value);
	}

	@Override
	public byte[] getRoot() {
		return parent.getRoot();
	}

	@Override
	public void garbageCollect(long commitNumber) {
		parent.garbageCollect(commitNumber);
	}
}