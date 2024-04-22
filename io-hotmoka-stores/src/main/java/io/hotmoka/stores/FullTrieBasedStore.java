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

package io.hotmoka.stores;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.stores.internal.TrieOfErrors;
import io.hotmoka.stores.internal.TrieOfHistories;
import io.hotmoka.stores.internal.TrieOfRequests;
import io.hotmoka.xodus.env.Transaction;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions, together with their
 * requests and errors (for this reason it is <i>full</i>).
 * This store has the ability of changing its <i>world view</i> by checking out different
 * hashes of its roots. Hence, it can be used to come back in time or change
 * history branch by simply checking out a different root. Its implementation
 * is based on Merkle-Patricia tries, supported by JetBrains' Xodus transactional database.
 * 
 * The information kept in this store consists of:
 * 
 * <ul>
 * <li> a trie that maps each Hotmoka request reference to the response computed for that request
 * <li> a trie that maps each storage reference to the transaction references that contribute
 *      to provide values to the fields of the storage object at that reference (its <i>history</i>);
 *      this is used by a node to reconstruct the state of the objects in store
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current number of commits
 * <li> a trie that maps each Hotmoka request reference to the corresponding request
 * <li> a trie that maps each Hotmoka request reference to the error that its execution generated
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 */
@ThreadSafe
public abstract class FullTrieBasedStore extends PartialStore implements CheckableStore {

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the errors of the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfErrors;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfRequests;

	/**
	 * The Xodus store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final io.hotmoka.xodus.env.Store storeOfHistory;

	/**
	 * The root of the trie of the errors. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfErrors = new byte[32];

	/**
	 * The root of the trie of the requests. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfRequests = new byte[32];

	/**
	 * The root of the trie of histories. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfHistories = new byte[32];

	/**
     * The trie of the errors.
     */
	private TrieOfErrors trieOfErrors;

	/**
     * The trie of the requests.
     */
	private TrieOfRequests trieOfRequests;

	/**
	 * The trie of histories.
	 */
	private TrieOfHistories trieOfHistories;

	/**
     * Creates the store. Its roots are not yet initialized. Hence, after this constructor,
	 * a call to {@link #setRootsTo(byte[])} or {@link #setRootsAsCheckedOut()}
	 * should occur, to set the roots of the store.
     * 
	 * @param getResponseUncommittedCached a function that yields the transaction response for the given transaction reference, if any, using a cache
     * @param dir the path where the database of the store gets created
     * @param checkableDepth the number of last commits that can be checked out, in order to
	 *                       change the world-view of the store (see {@link #checkout(byte[])}).
	 *                       This entails that such commits are not garbage-collected, until
	 *                       new commits get created on top and they end up being deeper.
	 *                       This is useful if we expect an old state to be checked out, for
	 *                       instance because a blockchain swaps to another history, but we can
	 *                       assume a reasonable depth for that to happen. Use 0 if the store
	 *                       is not checkable, so that all its successive commits can be immediately
	 *                       garbage-collected as soon as a new commit is created on top
	 *                       (which corresponds to a blockchain that never swaps to a previous
	 *                       state, because it has deterministic finality). Use a negative
	 *                       number if all commits must be checkable (hence garbage-collection
	 *                       is disabled)
     */
	protected FullTrieBasedStore(Function<TransactionReference, Optional<TransactionResponse>> getResponseUncommittedCached, Path dir, long checkableDepth) {
		super(getResponseUncommittedCached, dir, checkableDepth);

		AtomicReference<io.hotmoka.xodus.env.Store> storeOfErrors = new AtomicReference<>();
		AtomicReference<io.hotmoka.xodus.env.Store> storeOfRequests = new AtomicReference<>();
		AtomicReference<io.hotmoka.xodus.env.Store> storeOfHistory = new AtomicReference<>();

		env.executeInTransaction(txn -> {
			storeOfErrors.set(env.openStoreWithoutDuplicates("errors", txn));
			storeOfRequests.set(env.openStoreWithoutDuplicates("requests", txn));
			storeOfHistory.set(env.openStoreWithoutDuplicates("history", txn));
		});

		this.storeOfErrors = storeOfErrors.get();
		this.storeOfRequests = storeOfRequests.get();
		this.storeOfHistory = storeOfHistory.get();
	}

    @Override
	public Optional<String> getError(TransactionReference reference) {
    	synchronized (lock) {
    		return env.computeInReadonlyTransaction
   				(txn -> new TrieOfErrors(storeOfErrors, txn, nullIfEmpty(rootOfErrors), -1L).get(reference));
    	}
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		synchronized (lock) {
			return env.computeInReadonlyTransaction
				(txn -> new TrieOfRequests(storeOfRequests, txn, nullIfEmpty(rootOfRequests), -1L).get(reference));
		}
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) {
		synchronized (lock) {
			return env.computeInReadonlyTransaction
				(txn -> new TrieOfHistories(storeOfHistory, txn, nullIfEmpty(rootOfHistories), -1L).get(object));
		}
	}

	@Override
	public Stream<TransactionReference> getHistoryUncommitted(StorageReference object) {
		synchronized (lock) {
			return duringTransaction() ? trieOfHistories.get(object) : getHistory(object);
		}
	}

	@Override
	public void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		synchronized (lock) {
			trieOfRequests.put(reference, request);
			trieOfErrors.put(reference, errorMessage);
		}
	}

	@Override
	protected Transaction beginTransactionInternal() {
		synchronized (lock) {
			Transaction txn = super.beginTransactionInternal();
			long numberOfCommits = getNumberOfCommits();
			trieOfErrors = new TrieOfErrors(storeOfErrors, txn, nullIfEmpty(rootOfErrors), numberOfCommits);
			trieOfRequests = new TrieOfRequests(storeOfRequests, txn, nullIfEmpty(rootOfRequests), numberOfCommits);
			trieOfHistories = new TrieOfHistories(storeOfHistory, txn, nullIfEmpty(rootOfHistories), numberOfCommits);

			return txn;
		}
	}

	@Override
	protected void garbageCollect(long commitNumber) throws StoreException {
		super.garbageCollect(commitNumber);

		try {
			trieOfErrors.garbageCollect(commitNumber);
			trieOfRequests.garbageCollect(commitNumber);
			trieOfHistories.garbageCollect(commitNumber);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public void checkout(byte[] root) {
		synchronized (lock) {
			super.checkout(root);
		}
	}

	@Override
	protected void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		super.setResponse(reference, request, response);
	
		// we also store the request
		trieOfRequests.put(reference, request);
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		trieOfHistories.put(object, history);
	}

	@Override
	protected byte[] mergeRootsOfTries() throws StoreException {
		try {
			// this can be null if this is called before any new transaction has been executed over this store
			if (trieOfErrors == null)
				return super.mergeRootsOfTries();

			byte[] superMerge = super.mergeRootsOfTries();
			byte[] result = new byte[superMerge.length + 96];
			System.arraycopy(superMerge, 0, result, 0, superMerge.length);

			byte[] rootOfErrors = trieOfErrors.getRoot();
			if (rootOfErrors != null)
				System.arraycopy(rootOfErrors, 0, result, superMerge.length, 32);

			byte[] rootOfRequests = trieOfRequests.getRoot();
			if (rootOfRequests != null)
				System.arraycopy(rootOfRequests, 0, result, superMerge.length + 32, 32);

			byte[] rootOfHistories = trieOfHistories.getRoot();
			if (rootOfHistories != null)
				System.arraycopy(rootOfHistories, 0, result, superMerge.length + 64, 32);

			return result;
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void setRootsTo(byte[] root) {
		super.setRootsTo(root);

		if (root == null) {
			Arrays.fill(rootOfErrors, (byte) 0);
			Arrays.fill(rootOfRequests, (byte) 0);
			Arrays.fill(rootOfHistories, (byte) 0);
		}
		else {
			System.arraycopy(root, 64, rootOfErrors, 0, 32);
			System.arraycopy(root, 96, rootOfRequests, 0, 32);
			System.arraycopy(root, 128, rootOfHistories, 0, 32);
		}
	}

	@Override
	protected boolean isEmpty() {
		return super.isEmpty() && isEmpty(rootOfErrors) && isEmpty(rootOfRequests) && isEmpty(rootOfHistories);
	}
}