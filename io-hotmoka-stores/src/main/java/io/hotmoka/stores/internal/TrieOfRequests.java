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

import java.util.Optional;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.patricia.PatriciaTrie;
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
	 * @param garbageCollected true if and only if unused nodes must be garbage collected; in general,
	 *                         this can be true if previous configurations of the trie needn't be
	 *                         rechecked out in the future
	 */
	public TrieOfRequests(Store store, Transaction txn, byte[] root, boolean garbageCollected) {
		try {
			KeyValueStoreOnXodus keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithm.sha256(Marshallable::toByteArray);
			parent = PatriciaTrie.of(keyValueStoreOfResponses, new HashingForTransactionReference(), hashingForNodes, TransactionRequest::from, garbageCollected);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
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
}