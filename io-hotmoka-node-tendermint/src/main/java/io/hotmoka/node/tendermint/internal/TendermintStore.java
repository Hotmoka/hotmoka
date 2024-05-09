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

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractTrieBasedStore;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.xodus.env.Transaction;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its blocks.
 */
@Immutable
public class TendermintStore extends AbstractTrieBasedStore<TendermintStore, TendermintStoreTransaction> {

	/**
	 * The hasher used to merge the hashes of the many tries.
	 */
	private final Hasher<byte[]> hasherOfHashes;

	private final Supplier<TendermintPoster> poster;

	/**
     * Creates a store for the Tendermint blockchain.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node an object that can be used to send post requests to Tendermint
     */
    TendermintStore(Supplier<TendermintPoster> poster, ExecutorService executors, ConsensusConfig<?,?> consensus, TendermintNodeConfig config, Hasher<TransactionRequest<?>> hasher) {
    	super(executors, consensus, config, hasher);

    	this.poster = poster;

    	try {
    		this.hasherOfHashes = HashingAlgorithms.sha256().getHasher(Function.identity());
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw new RuntimeException("unexpected exception", e);
    	}
    }

    private TendermintStore(TendermintStore toClone, LRUCache<TransactionReference, Boolean> checkedSignatures, LRUCache<TransactionReference, EngineClassLoader> classLoaders, ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests) {
    	super(toClone, checkedSignatures, classLoaders, consensus, gasPrice, inflation, rootOfResponses, rootOfInfo, rootOfErrors, rootOfHistories, rootOfRequests);

    	this.hasherOfHashes = toClone.hasherOfHashes;
    	this.poster = toClone.poster;
	}

	@Override
	public Optional<String> getError(TransactionReference reference) {
    	// error messages are held inside the Tendermint blockchain
    	return poster.get().getErrorMessage(reference.getHash());
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		// requests are held inside the Tendermint blockchain
		return poster.get().getRequest(reference.getHash());
	}

	/**
	 * Yields the hash of this store. It is computed from the roots of its tries.
	 * 
	 * @return the hash. If the store is currently empty, it yields an empty array of bytes
	 */
	byte[] getHash() {
		try {
			return isEmpty() ?
					new byte[0] : // Tendermint requires an empty array at the beginning, for consensus
						// we do not use the info part of the hash, so that the hash
						// remains stable when the responses and the histories are stable,
						// although the info part has changed for the update of the number of commits
						hasherOfHashes.hash(mergeRootsOfTriesWithoutInfo()); // we hash the result into 32 bytes
		}
		catch (StoreException e) {
			throw new RuntimeException(e); // TODO
		}
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

	@Override
    protected TendermintStore mkClone(LRUCache<TransactionReference, Boolean> checkedSignatures, LRUCache<TransactionReference, EngineClassLoader> classLoaders, ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests) {
		return new TendermintStore(this, checkedSignatures, classLoaders, consensus, gasPrice, inflation, rootOfResponses, rootOfInfo, rootOfErrors, rootOfHistories, rootOfRequests);
	}

	@Override
	protected TendermintStoreTransaction mkTransaction(Transaction txn, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		return new TendermintStoreTransaction(this, executors, consensus, now, txn);
	}
}