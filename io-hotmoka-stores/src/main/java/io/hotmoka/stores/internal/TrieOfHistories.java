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