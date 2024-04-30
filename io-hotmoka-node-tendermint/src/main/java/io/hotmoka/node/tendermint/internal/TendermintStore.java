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

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Function;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.stores.AbstractTrieBasedStore;
import io.hotmoka.stores.StoreException;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its blocks.
 */
@ThreadSafe
public class TendermintStore extends AbstractTrieBasedStore<TendermintStore> {

	/**
	 * An object that can be used to send post requests to Tendermint
	 */
	private final TendermintNodeInternal nodeInternal;

	/**
	 * The hasher used to merge the hashes of the many tries.
	 */
	private final Hasher<byte[]> hasherOfHashes;

	/**
     * Creates a store for the Tendermint blockchain.
     * It is initialized to the view of the last checked out root.
     * 
	 * @param dir the path where the database of the store gets created
     * @param nodeInternal an object that can be used to send post requests to Tendermint
     */
    TendermintStore(Path dir, TendermintNodeInternal nodeInternal) {
    	super(dir);

    	this.nodeInternal = nodeInternal;

    	try {
    		this.hasherOfHashes = HashingAlgorithms.sha256().getHasher(Function.identity());
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw new RuntimeException("unexpected exception", e);
    	}
    }

    private TendermintStore(TendermintStore toClone) {
    	super(toClone);
 
    	this.nodeInternal = toClone.nodeInternal;
    	this.hasherOfHashes = toClone.hasherOfHashes;
    }

    private TendermintStore(TendermintStore toClone, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests) {
    	super(toClone, rootOfResponses, rootOfInfo, rootOfErrors, rootOfHistories, rootOfRequests);

    	this.nodeInternal = toClone.nodeInternal;
    	this.hasherOfHashes = toClone.hasherOfHashes;
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

	/**
	 * Yields the hash of this store. It is computed from the roots of its tries.
	 * 
	 * @return the hash. If the store is currently empty, it yields an empty array of bytes
	 */
	byte[] getHash() {
		try {
		synchronized (lock) {
			return isEmpty() ?
				new byte[0] : // Tendermint requires an empty array at the beginning, for consensus
				// we do not use the info part of the hash, so that the hash
				// remains stable when the responses and the histories are stable,
				// although the info part has changed for the update of the number of commits
				hasherOfHashes.hash(mergeRootsOfTriesWithoutInfo()); // we hash the result into 32 bytes
		}
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
		byte[] bytes = mergeRootsOfTries();
		for (int pos = 32; pos < 64; pos++)
			bytes[pos] = 0;

		return bytes;
	}

	@Override
	protected TendermintStore getThis() {
		return this;
	}

	@Override
	protected TendermintStore mkClone() {
		return new TendermintStore(this);
	}

	@Override
    protected TendermintStore mkClone(Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests) {
		return new TendermintStore(this, rootOfResponses, rootOfInfo, rootOfErrors, rootOfHistories, rootOfRequests);
	}

	@Override
	protected TendermintStore setRequest(TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		// nothing to do, since Tendermint keeps requests inside its blockchain
		return this;
	}

	@Override
	protected TendermintStore setError(TransactionReference reference, String error) throws StoreException {
		// nothing to do, since Tendermint keeps error messages inside the blockchain, in the field "data" of its transactions
		return this;
	}

	@Override
	protected TendermintStoreTransaction mkTransaction() {
		return new TendermintStoreTransaction(this, lock);
	}
}