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

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.MarshallableBean;
import io.hotmoka.beans.BeanMarshallingContext;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.patricia.PatriciaTrie;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A map from storage references to an array of transaction references (their <i>history</i>),
 * backed by a Merkle-Patricia trie.
 */
public class TrieOfHistories {

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<StorageReference, MarshallableArrayOfTransactionReferences> parent;

	/**
	 * Builds a Merkle-Patricia trie that maps references to storage references into
	 * an array of transaction references (their <i>history</i>).
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data must be garbage collected (eventually)
	 *                        as result of the store updates performed during that commit; you can pass
	 *                        -1L if the trie is used only for reading
	 */
	public TrieOfHistories(Store store, Transaction txn, byte[] root, long numberOfCommits) {
		try {
			KeyValueStoreOnXodus keyValueStoreOfHistories = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithms.sha256(Marshallable::toByteArray);
			HashingAlgorithm<StorageReference> hashingForStorageReferences = HashingAlgorithms.sha256(StorageReference::toByteArrayWithoutSelector);
			parent = PatriciaTrie.of(keyValueStoreOfHistories, hashingForStorageReferences, hashingForNodes, MarshallableArrayOfTransactionReferences::from, numberOfCommits);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("unexpected exception", e);
		}
	}

	public Stream<TransactionReference> get(StorageReference key) {
		Optional<MarshallableArrayOfTransactionReferences> result = parent.get(key);
		if (result.isEmpty())
			return Stream.empty();

		TransactionReference[] transactions = result.get().transactions;
		// histories always end with the transaction that created the object,
		// hence with the transaction of the same storage reference of the object
		TransactionReference[] withLast = new TransactionReference[transactions.length + 1];
		System.arraycopy(transactions, 0, withLast, 0, transactions.length);
		withLast[transactions.length] = key.transaction;
		return Stream.of(withLast);
	}

	public void put(StorageReference key, Stream<TransactionReference> history) {
		// we do not keep the last transaction, since the history of an object always ends
		// with the transaction that created the object, that is, with the same transaction
		// of the storage reference of the object
		TransactionReference[] transactionsAsArray = history.toArray(TransactionReference[]::new);
		TransactionReference[] withoutLast = new TransactionReference[transactionsAsArray.length - 1];
		System.arraycopy(transactionsAsArray, 0, withoutLast, 0, withoutLast.length);
		parent.put(key, new MarshallableArrayOfTransactionReferences(withoutLast));
	}

	public byte[] getRoot() {
		return parent.getRoot();
	}

	/**
	 * An array of transaction references that can be marshalled into an object stream.
	 */
	private static class MarshallableArrayOfTransactionReferences extends MarshallableBean {
		private final TransactionReference[] transactions;

		private MarshallableArrayOfTransactionReferences(TransactionReference[] transactions) {
			this.transactions = transactions.clone();
		}

		@Override
		public void into(BeanMarshallingContext context) throws IOException {
			// we do not try to share repeated transaction references, since they do not occur in histories
			// and provision for sharing would just make the size of the histories larger
			context.writeCompactInt(transactions.length);
			for (TransactionReference reference: transactions)
				context.write(reference.getHashAsBytes());
		}

		/**
		 * Factory method that unmarshals an array of transaction references from the given stream.
		 * 
		 * @param context the unmarshalling context
		 * @return the array
		 * @throws IOException if the array could not be unmarshalled
		 * @throws ClassNotFoundException if the array could not be unmarshalled
		 */
		private static MarshallableArrayOfTransactionReferences from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
			int size = TransactionRequest.REQUEST_HASH_LENGTH;
			
			// we do not share repeated transaction references, since they do not occur in histories
			// and provision for sharing would just make the size of the histories larger
			return new MarshallableArrayOfTransactionReferences(context.readArray
				(_context -> new LocalTransactionReference(_context.readBytes(size, "inconsistent length of transaction reference")),
				TransactionReference[]::new));
		}

		@Override
		protected BeanMarshallingContext createMarshallingContext(OutputStream os) throws IOException {
			return new BeanMarshallingContext(os);
		}
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