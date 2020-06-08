package io.hotmoka.beans.references;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * A transaction reference that refers to a transaction in the local store of a node.
 */
public final class LocalTransactionReference extends TransactionReference {

	/**
	 * The hash of the request that generated the transaction.
	 */
	public final String hash;

	/**
	 * Builds a transaction reference.
	 * 
	 * @param hash the hash of the transaction
	 */
	public LocalTransactionReference(String hash) {
		this.hash = hash;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof LocalTransactionReference && ((LocalTransactionReference) other).getHash().equals(hash);
	}

	@Override
	public int hashCode() {
		return hash.hashCode();
	}

	@Override
	public String toString() {
		return hash;
	}

	@Override
	public int compareTo(TransactionReference other) {
		return hash.compareTo(other.getHash());
	}

	@Override
	public String getHash() {
		return hash;
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		oos.writeObject(hash);
	}
}