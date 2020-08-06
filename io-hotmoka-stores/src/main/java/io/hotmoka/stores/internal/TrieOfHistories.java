package io.hotmoka.stores.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.HashingAlgorithm;
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
	 */
	public TrieOfHistories(Store store, Transaction txn, byte[] root) {
		try {
			KeyValueStoreOnXodus keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithm.sha256(Marshallable::toByteArray);
			HashingAlgorithm<StorageReference> hashingForStorageReferences = HashingAlgorithm.sha256(StorageReference::toByteArrayWithoutSelector);
			parent = PatriciaTrie.of(keyValueStoreOfResponses, hashingForStorageReferences, hashingForNodes, MarshallableArrayOfTransactionReferences::from);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}

	public Stream<TransactionReference> get(StorageReference key) {
		Optional<MarshallableArrayOfTransactionReferences> result = parent.get(key);
		return result.isEmpty() ? Stream.empty() : Stream.of(result.get().transactions);
	}

	public void put(StorageReference key, Stream<TransactionReference> history) {
		parent.put(key, new MarshallableArrayOfTransactionReferences(history.toArray(TransactionReference[]::new)));
	}

	public byte[] getRoot() {
		return parent.getRoot();
	}

	/**
	 * An array of transaction references that can be marshalled into an object stream.
	 */
	private static class MarshallableArrayOfTransactionReferences extends Marshallable {
		private final TransactionReference[] transactions;

		private MarshallableArrayOfTransactionReferences(TransactionReference[] transactions) {
			this.transactions = transactions.clone();
		}

		@Override
		public void into(MarshallingContext context) throws IOException {
			intoArray(transactions, context);
		}

		/**
		 * Factory method that unmarshals an array of transaction references from the given stream.
		 * 
		 * @param ois the stream
		 * @return the array
		 * @throws IOException if the array could not be unmarshalled
		 * @throws ClassNotFoundException if the array could not be unmarshalled
		 */
		private static MarshallableArrayOfTransactionReferences from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			return new MarshallableArrayOfTransactionReferences(Marshallable.unmarshallingOfArray(TransactionReference::from, TransactionReference[]::new, ois));
		}
	}
}