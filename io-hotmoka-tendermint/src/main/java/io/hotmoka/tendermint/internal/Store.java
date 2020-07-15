package io.hotmoka.tendermint.internal;

import java.util.Optional;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.stores.PartialTrieBasedStore;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its block.
 */
class Store extends PartialTrieBasedStore<TendermintBlockchainImpl> {

	/**
     * Creates a state for the Tendermint blockchain.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node the node for which the state is being built
     */
    Store(TendermintBlockchainImpl node) {
    	super(node);
    }

    /**
     * Creates a state for the Tendermint blockchain.
     * It is initialized to the view of the given root.
     * 
	 * @param node the node for which the state is being built
     * @param hash the root to use for the state
     */
    Store(TendermintBlockchainImpl node, byte[] hash) {
    	super(node, hash);
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
}