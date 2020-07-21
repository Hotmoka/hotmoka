package io.hotmoka.stores.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.patricia.PatriciaTrie;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A map from transaction requests into their error, backed by a Merkle-Patricia trie.
 */
public class TrieOfErrors {

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<TransactionReference, MarshallableString> parent;

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
	 * Builds a Merkle-Patricia trie that maps transaction requests into their errors.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 */
	public TrieOfErrors(Store store, Transaction txn, byte[] root) {
		try {
			KeyValueStoreOnXodus keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithm.sha256(Marshallable::toByteArray);
			parent = PatriciaTrie.of(keyValueStoreOfResponses, hashingForTransactionReferences, hashingForNodes, MarshallableString::from);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}

	public Optional<String> get(TransactionReference key) {
		Optional<MarshallableString> result = parent.get(key);
		if (result.isPresent())
			return Optional.of(result.get().toString());
		else
			return Optional.empty();
	}

	public void put(TransactionReference key, String value) {
		parent.put(key, new MarshallableString(value));
	}

	public byte[] getRoot() {
		return parent.getRoot();
	}

	/**
	 * A string that can be marshalled into an object stream.
	 */
	private static class MarshallableString extends Marshallable {
		private final String s;

		private MarshallableString(String s) {
			this.s = s;
		}

		@Override
		public void into(ObjectOutputStream oos) throws IOException {
			oos.writeUTF(s);
		}

		@Override
		public String toString() {
			return s;
		}

		/**
		 * Factory method that unmarshals a string from the given stream.
		 * 
		 * @param ois the stream
		 * @return the string
		 * @throws IOException if the string could not be unmarshalled
		 */
		private static MarshallableString from(ObjectInputStream ois) throws IOException {
			return new MarshallableString(ois.readUTF());
		}
	}
}