package io.hotmoka.stores.internal;

import java.math.BigInteger;
import java.util.Optional;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.patricia.PatriciaTrie;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A Merkle-Patricia trie that maps miscellaneous information into their value.
 */
public class TrieOfInfo {

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<Byte, StorageValue> parent;

	/**
	 * The hashing algorithm applied to the keys of the trie.
	 */
	private final HashingAlgorithm<Byte> hashingForKeys = new HashingAlgorithm<>() {

		@Override
		public byte[] hash(Byte key) {
			return new byte[] { key.byteValue() };
		}

		@Override
		public int length() {
			return 1;
		}
	};

	/**
	 * Builds a Merkle-Patricia trie that maps miscellaneous information into their value.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 */
	public TrieOfInfo(Store store, Transaction txn, byte[] root) {
		try {
			KeyValueStoreOnXodus keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithm.sha256(Marshallable::toByteArray);
			parent = PatriciaTrie.of(keyValueStoreOfResponses, hashingForKeys, hashingForNodes, StorageValue::from);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the root of the trie, that can be used as a hash of the trie itself.
	 * 
	 * @return the root
	 */
	public byte[] getRoot() {
		return parent.getRoot();
	}

	/**
	 * Yields the number of commits.
	 * 
	 * @return the number of commits. This is 0 if the number of commits has not been set yet
	 */
	public BigInteger getNumberOfCommits() {
		return parent.get((byte) 0)
			.map(commits -> ((BigIntegerValue) commits).value)
			.orElse(BigInteger.ZERO);
	}

	/**
	 * Sets the number of commits.
	 * 
	 * @param num the number to set
	 */
	public void setNumberOfCommits(BigInteger num) {
		parent.put((byte) 0, new BigIntegerValue(num));
	}

	/**
	 * Yields the manifest.
	 * 
	 * @return the manifest, if any
	 */
	public Optional<StorageReference> getManifest() {
		return parent.get((byte) 1)
			.map(manifest -> (StorageReference) manifest);
	}

	/**
	 * Sets the manifest.
	 * 
	 * @param manifest the manifest to set
	 */
	public void setManifest(StorageReference manifest) {
		parent.put((byte) 1, manifest);
	}
}