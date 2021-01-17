package io.hotmoka.tendermint.internal;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.stores.PartialTrieBasedFlatHistoryStore;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its blocks.
 */
@ThreadSafe
class Store extends PartialTrieBasedFlatHistoryStore<TendermintBlockchainImpl> {

	/**
	 * The hashing algorithm used to merge the hashes of the many tries.
	 */
	private final HashingAlgorithm<byte[]> hashOfHashes;

	/**
     * Creates a store for the Tendermint blockchain.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node the node for which the store is being built
     */
    Store(TendermintBlockchainImpl node) {
    	super(node);

    	AtomicReference<io.hotmoka.xodus.env.Store> storeOfConfig = new AtomicReference<>();

    	recordTime(() -> env.executeInTransaction(txn -> {
    		storeOfConfig.set(env.openStoreWithoutDuplicates("config", txn));
    	}));

    	setRootsAsCheckedOut();

    	try {
    		this.hashOfHashes = HashingAlgorithm.sha256((byte[] bytes) -> bytes);
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw InternalFailureException.of(e);
    	}
    }

    @Override
	public Optional<String> getError(TransactionReference reference) {
		try {
			// error messages are held inside the Tendermint blockchain
			return node.getTendermint().getErrorMessage(reference.getHash());
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		try {
			// requests are held inside the Tendermint blockchain
			return node.getTendermint().getRequest(reference.getHash());
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
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
}