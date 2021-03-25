package io.hotmoka.beans.references;

import java.io.IOException;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.requests.TransactionRequest;

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
		if (hash == null)
			throw new IllegalArgumentException("hash cannot be null");

		// each byte is represented by two successive characters
		if (hash.length() != TransactionRequest.hashingForRequests.length() * 2)
			throw new IllegalArgumentException("illegal transaction reference " + hash
				+ ": it should hold a hash of " + TransactionRequest.hashingForRequests.length() * 2 + " characters");

		hash = hash.toLowerCase();

		if (!hash.chars().allMatch(c -> (c >= '0' && c <='9') || (c >= 'a' && c <= 'f')))
			throw new IllegalArgumentException("illegal transaction reference " + hash + ": it must be a hexadecimal number");

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
	public void into(MarshallingContext context) throws IOException {
		context.writeTransactionReference(this);
	}
}