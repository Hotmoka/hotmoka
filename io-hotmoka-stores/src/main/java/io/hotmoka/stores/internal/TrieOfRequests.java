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
	 * The hashing algorithm applied to transaction references when used as
	 * keys of the trie. Since these keys are transaction references,
	 * they already hold a hash, as a string. Hence, this algorithm just amounts to extracting
	 * the bytes from that string.
	 */
	private final HashingAlgorithm<TransactionReference> hashingForTransactionReferences = new HashingAlgorithm<>() {
	
		@Override
		public byte[] hash(TransactionReference reference) {
			return hexStringToByteArray(reference.getHash());
		}
	
		@Override
		public int length() {
			return 32; // transaction references are assumed to be SHA256 hashes, hence 32 bytes
		}
	
		/**
		 * Transforms a hexadecimal string into a byte array.
		 * 
		 * @param s the string
		 * @return the byte array
		 */
		private byte[] hexStringToByteArray(String s) {
		    int len = s.length();
		    byte[] data = new byte[len / 2];
		    for (int i = 0; i < len; i += 2)
		        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		
		    return data;
		}
	};

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
			parent = PatriciaTrie.of(keyValueStoreOfResponses, hashingForTransactionReferences, hashingForNodes, TransactionRequest::from, garbageCollected);
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