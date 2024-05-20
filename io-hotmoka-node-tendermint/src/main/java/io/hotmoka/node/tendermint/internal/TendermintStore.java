/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.tendermint.internal;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractTrieBasedStore;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its blocks.
 */
@Immutable
public class TendermintStore extends AbstractTrieBasedStore<TendermintStore, TendermintStoreTransformation> {

	/**
	 * The hasher used to merge the hashes of the many tries.
	 */
	private final Hasher<byte[]> hasherOfHashes;

	/**
	 * The current validators set in this store transaction. This information could be recovered from the store transaction itself,
	 * but this field is used for caching. The validators set might be missing if the node is not initialized yet.
	 */
	private volatile Optional<TendermintValidator[]> validators;

	/**
     * Creates a store for the Tendermint blockchain.
	 * 
	 * @param executors the executors to use for running transactions
	 * @param consensus the consensus configuration of the node having the store
	 * @param config the local configuration of the node having the store
	 * @param hasher the hasher for computing the transaction reference from the requests
	 */
    TendermintStore(ExecutorService executors, ConsensusConfig<?,?> consensus, TendermintNodeConfig config, Hasher<TransactionRequest<?>> hasher) throws StoreException {
    	super(executors, consensus, config, hasher);

    	try {
    		this.hasherOfHashes = HashingAlgorithms.sha256().getHasher(Function.identity());
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw new StoreException(e);
    	}

    	this.validators = Optional.empty();
    }

	/**
	 * Creates a clone of a store.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 * @param rootOfResponses the root to use for the tries of responses
	 * @param rootOfInfo the root to use for the tries of infos
	 * @param rootOfHistories the root to use for the tries of histories
	 * @param rootOfRequests the root to use for the tries of requests
	 */
    private TendermintStore(TendermintStore toClone, StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
    	super(toClone, cache, rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests);

    	this.hasherOfHashes = toClone.hasherOfHashes;
    	this.validators = toClone.validators;
	}

	/**
	 * Creates a clone of a store.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 */
    private TendermintStore(TendermintStore toClone, StoreCache cache) {
    	super(toClone, cache);

    	this.hasherOfHashes = toClone.hasherOfHashes;
    	this.validators = toClone.validators;
	}

    @Override
    protected TendermintStore addDelta(StoreCache cache, Map<TransactionReference, TransactionRequest<?>> addedRequests,
    		Map<TransactionReference, TransactionResponse> addedResponses,
    		Map<StorageReference, TransactionReference[]> addedHistories, Optional<StorageReference> addedManifest)
    		throws StoreException {

    	TendermintStore result = super.addDelta(cache, addedRequests, addedResponses, addedHistories, addedManifest);
    	result.validators = validators;
    	return result;
    }

    @Override
    protected TendermintStore setCache(StoreCache cache) {
    	return new TendermintStore(this, cache);
    }

    /**
     * Yields the hash of this store. It is computed from the roots of its tries.
     * 
     * @return the hash. If the store is currently empty, it yields an empty array of bytes
     */
    protected byte[] getHash() throws StoreException {
    	if (isEmpty())
    		return new byte[0]; // Tendermint requires an empty array at the beginning, for consensus
    	else
    		// we do not use the info part of the hash, so that the hash
    		// remains stable when the responses and the histories are stable,
    		// although the info part has changed for the update of the number of commits
    		return hasherOfHashes.hash(mergeRootsOfTriesWithoutInfo()); // we hash the result into 32 bytes
    }

	@Override
    protected TendermintStore make(StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
		return new TendermintStore(this, cache, rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests);
	}

	@Override
	protected TendermintStoreTransformation beginTransformation(ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		return new TendermintStoreTransformation(this, executors, consensus, now, validators);
	}

	/**
	 * Yields the concatenation of the roots of the tries in this store,
	 * with the exclusion of the info trie, whose root is masked with 0's.
	 * This is a trick to make the hash independent from the number of commits
	 * stored in the info tries. In this way, Tendermint will eventually pause creating
	 * blocks if the only different between states is the number of commits.
	 * 
	 * @return the concatenation
	 * @throws TrieException 
	 */
	private byte[] mergeRootsOfTriesWithoutInfo() throws StoreException {
		byte[] bytes = getStateId();
		for (int pos = 32; pos < 64; pos++)
			bytes[pos] = 0;
	
		return bytes;
	}
}