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
import java.util.stream.Stream;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A map from storage references to an array of transaction references (their <i>history</i>),
 * backed by a Merkle-Patricia trie.
 */
public class TrieOfHistories extends AbstractPatriciaTrie<StorageReference, Stream<TransactionReference>> {

	/**
	 * Builds a Merkle-Patricia trie that maps references to storage references into
	 * an array of transaction references (their <i>history</i>).
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use empty to create the empty trie
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data must be garbage collected (eventually)
	 *                        as result of the store updates performed during that commit; you can pass
	 *                        -1L if the trie is used only for reading
	 */
	public TrieOfHistories(Store store, Transaction txn, Optional<byte[]> root, long numberOfCommits) {
		super(new KeyValueStoreOnXodus(store, txn), root, sha256().getHasher(StorageReference::toByteArrayWithoutSelector),
			sha256(), s -> new MarshallableArrayOfTransactionReferences(s.toArray(TransactionReference[]::new)).toByteArray(),
			bytes -> Stream.of(MarshallableArrayOfTransactionReferences.from(NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))).transactions), numberOfCommits);
	}

	private static HashingAlgorithm sha256() {
		try {
			return HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	@Override
	public Optional<Stream<TransactionReference>> get(StorageReference key) throws TrieException {
		Optional<Stream<TransactionReference>> result = super.get(key);
		if (result.isEmpty())
			return Optional.empty();

		TransactionReference[] transactions = result.get().toArray(TransactionReference[]::new);
		// histories always end with the transaction that created the object,
		// hence with the transaction of the same storage reference of the object
		var withLast = new TransactionReference[transactions.length + 1];
		System.arraycopy(transactions, 0, withLast, 0, transactions.length);
		withLast[transactions.length] = key.getTransaction();
		return Optional.of(Stream.of(withLast));
	}

	@Override
	public void put(StorageReference key, Stream<TransactionReference> history) throws TrieException {
		// we do not keep the last transaction, since the history of an object always ends
		// with the transaction that created the object, that is, with the same transaction
		// of the storage reference of the object
		var transactionsAsArray = history.toArray(TransactionReference[]::new);
		var withoutLast = new TransactionReference[transactionsAsArray.length - 1];
		System.arraycopy(transactionsAsArray, 0, withoutLast, 0, withoutLast.length);
		super.put(key, Stream.of(withoutLast));
	}
}