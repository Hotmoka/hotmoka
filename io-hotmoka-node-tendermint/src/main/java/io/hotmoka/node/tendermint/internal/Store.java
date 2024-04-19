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
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.stores.PartialTrieBasedWithHistoryStore;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its blocks.
 */
@ThreadSafe
class Store extends PartialTrieBasedWithHistoryStore {

	/**
	 * The node having this store.
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
     * @param getResponseUncommittedCached a function that yields the transaction response for the given transaction reference, if any, using a cache
	 * @param dir the path where the database of the store gets created
     * @param nodeInternal the same node, with internal methods
     */
    Store(Function<TransactionReference, Optional<TransactionResponse>> getResponseUncommittedCached, Path dir, TendermintNodeInternal nodeInternal) {
    	super(getResponseUncommittedCached, dir, 0L); // 0L since this blockchain enjoys deterministic finality: we will never checkout an old state

    	this.nodeInternal = nodeInternal;

    	setRootsAsCheckedOut();

    	try {
    		this.hasherOfHashes = HashingAlgorithms.sha256().getHasher(Function.identity());
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw new RuntimeException("unexpected exception", e);
    	}
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
				// we do not use the info part of the hash, so that the hash
				// remains stable when the responses and the histories are stable,
				// although the info part has changed for the update of the number of commits
				hasherOfHashes.hash(mergeRootsOfTriesWithoutInfo()); // we hash the result into 32 bytes
		}
	}

	/**
	 * Yields the concatenation of the roots of the tries in this store,
	 * with the exclusion of the info trie, whose root is masked with 0's.
	 * 
	 * @return the concatenation
	 */
	private byte[] mergeRootsOfTriesWithoutInfo() {
		byte[] bytes = mergeRootsOfTries();
		for (int pos = 32; pos < 64; pos++)
			bytes[pos] = 0;

		return bytes;
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