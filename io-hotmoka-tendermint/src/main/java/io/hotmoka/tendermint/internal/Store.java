package io.hotmoka.tendermint.internal;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.stores.PartialTrieBasedWithHistoryStore;
import io.hotmoka.tendermint.TendermintBlockchainConfig;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its blocks.
 */
@ThreadSafe
class Store extends PartialTrieBasedWithHistoryStore<TendermintBlockchainConfig> {

	/**
	 * The node having this store.
	 */
	private final TendermintBlockchainInternal nodeInternal;

	/**
	 * The hashing algorithm used to merge the hashes of the many tries.
	 */
	private final HashingAlgorithm<byte[]> hashOfHashes;

	/**
     * Creates a store for the Tendermint blockchain.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node the node having this store
     * @param nodeInternal the same node, with internal methods
     */
    Store(TendermintBlockchainImpl node, TendermintBlockchainInternal nodeInternal) {
    	super(node);

    	this.nodeInternal = nodeInternal;

    	setRootsAsCheckedOut();

    	try {
    		this.hashOfHashes = HashingAlgorithm.sha256(bytes -> bytes);
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw InternalFailureException.of(e);
    	}
    }

    /**
     * Creates a clone of the given store.
     * 
     * @param parent the store to clone
     */
    Store(Store parent) {
    	super(parent);

    	this.nodeInternal = parent.nodeInternal;
    	this.hashOfHashes = parent.hashOfHashes;
    }

    @Override
    public Store copy() {
    	return new Store(this);
    }

    @Override
	public Optional<String> getError(TransactionReference reference) {
    	// error messages are held inside the Tendermint blockchain
    	return nodeInternal.getPoster().getErrorMessage(reference.getHash());
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		// requests are held inside the Tendermint blockchain
		return nodeInternal.getPoster().getRequest(reference.getHash());
	}

	@Override
	public void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		// nothing to do, since Tendermint keeps error messages inside the blockchain, in the field "data" of its transactions
	}

 	/**
	 * Yields the hash of this store. It is computed from the roots of its tries.
	 * 
	 * @return the hash. If the store is currently empty, it yields an empty array of bytes
	 */
	byte[] getHash() {
		synchronized (lock) {
			return isEmpty() ?
				new byte[0] : // Tendermint requires an empty array at the beginning, for consensus
				hashOfHashes.hash(mergeRootsOfTries()); // we hash the result into 32 bytes
		}
	}

	/**
	 * Commits the current transaction and checks it out, so that it becomes
	 * the current view of the world of this store.
	 */
	final void commitTransactionAndCheckout() {
		synchronized (lock) {
			checkout(commitTransaction());
		}
	}
}