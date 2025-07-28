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

import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.UnknownKeyException;

/**
 * A map from storage references to an array of transaction references (their <i>history</i>),
 * backed by a Merkle-Patricia trie.
 * It uses the sha256 hashing algorithm for the trie's nodes and an array of 0's to represent
 * the empty trie.
 */
public class TrieOfHistories extends AbstractPatriciaTrie<StorageReference, Stream<TransactionReference>, TrieOfHistories> {

	/**
	 * Builds a Merkle-Patricia trie that maps references to storage references into
	 * an array of transaction references (their <i>history</i>).
	 * 
	 * @param store the supporting key/value store
	 * @param root the root of the trie to check out; use empty to create the empty trie
	 * @param node the node for which the trie is being built
	 * @throws UnknownKeyException if {@code root} cannot be found in the trie
	 */
	public TrieOfHistories(KeyValueStore store, byte[] root, AbstractTrieBasedLocalNodeImpl<?,?,?,?> node) throws UnknownKeyException {
		super(store, root, mkSHA256().getHasher(StorageReference::toByteArrayWithoutSelector),
			mkSHA256(), new byte[32], TrieOfHistories::historyToBytes, TrieOfHistories::bytesToHistory);
	}

	private TrieOfHistories(TrieOfHistories cloned, byte[] root) throws UnknownKeyException {
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

	private static byte[] historyToBytes(Stream<TransactionReference> history) {
		var toConcat = history.toArray(TransactionReference[]::new);
		int requestHashLength = TransactionReference.REQUEST_HASH_LENGTH;
		byte[] result = new byte[toConcat.length * requestHashLength];

		int pos = 0;
		for (TransactionReference reference: toConcat) {
			System.arraycopy(reference.getHash(), 0, result, pos, requestHashLength);
			pos += requestHashLength;
		}

		return result;
	}

	private static Stream<TransactionReference> bytesToHistory(byte[] bytes) {
		int requestHashLength = TransactionReference.REQUEST_HASH_LENGTH;
		var references = new TransactionReference[bytes.length / requestHashLength];
		for (int index = 0, pos = 0; pos < bytes.length; pos += requestHashLength, index++) {
			var hash = new byte[requestHashLength];
			System.arraycopy(bytes, pos, hash, 0, requestHashLength);
			references[index] = TransactionReferences.of(hash);
		}
		
		return Stream.of(references);
	}

	@Override
	public Optional<Stream<TransactionReference>> get(StorageReference key) {
		Optional<Stream<TransactionReference>> result = super.get(key);
		if (result.isEmpty())
			return Optional.empty();

		var transactions = result.get().toArray(TransactionReference[]::new);
		// histories always end with the transaction that created the object,
		// hence with the transaction of the storage reference of the object;
		// that last transaction is not stored, since it can be implicitly recovered
		var withLast = new TransactionReference[transactions.length + 1];
		System.arraycopy(transactions, 0, withLast, 0, transactions.length);
		withLast[transactions.length] = key.getTransaction();
		return Optional.of(Stream.of(withLast));
	}

	@Override
	public TrieOfHistories put(StorageReference key, Stream<TransactionReference> history) {
		// we do not keep the last transaction, since the history of an object always ends
		// with the transaction that created the object, that is, with the same transaction
		// of the storage reference of the object
		var transactionsAsArray = history.toArray(TransactionReference[]::new);
		var withoutLast = new TransactionReference[transactionsAsArray.length - 1];
		System.arraycopy(transactionsAsArray, 0, withoutLast, 0, withoutLast.length);
		return super.put(key, Stream.of(withoutLast));
	}

	@Override
	public TrieOfHistories checkoutAt(byte[] root) throws UnknownKeyException {
		return new TrieOfHistories(this, root);
	}
}