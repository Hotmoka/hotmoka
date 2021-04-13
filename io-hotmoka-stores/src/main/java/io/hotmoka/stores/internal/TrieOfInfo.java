package io.hotmoka.stores.internal;

import java.util.Optional;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.values.LongValue;
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
	 * Builds a Merkle-Patricia trie that maps miscellaneous information into their value.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 * @param garbageCollected true if and only if unused nodes must be garbage collected; in general,
	 *                         this can be true if previous configurations of the trie needn't be
	 *                         rechecked out in the future
	 */
	public TrieOfInfo(Store store, Transaction txn, byte[] root, boolean garbageCollected) {
		try {
			KeyValueStoreOnXodus keyValueStoreOfInfos = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithm.sha256(Marshallable::toByteArray);

			// the hashing algorithm applied to the keys of the trie.
			HashingAlgorithm<Byte> hashingForKeys = new HashingAlgorithm<>() {

				@Override
				public byte[] hash(Byte key) {
					return new byte[] { key };
				}

				@Override
				public int length() {
					return 1;
				}
			};

			parent = PatriciaTrie.of(keyValueStoreOfInfos, hashingForKeys, hashingForNodes, StorageValue::from, garbageCollected);
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
	public long getNumberOfCommits() {
		return parent.get((byte) 0)
			.map(commits -> ((LongValue) commits).value)
			.orElse(0L);
	}

	/**
	 * Increases the number of commits.
	 */
	public void increaseNumberOfCommits() {
		parent.put((byte) 0, new LongValue(getNumberOfCommits() + 1));
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