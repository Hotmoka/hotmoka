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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.stores.internal.KeyValueStoreOnXodus;
import io.hotmoka.stores.internal.TrieOfErrors;
import io.hotmoka.stores.internal.TrieOfHistories;
import io.hotmoka.stores.internal.TrieOfRequests;
import io.hotmoka.xodus.env.Transaction;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions, together with their
 * requests, histories and errors (for this reason it is <i>full</i>).
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
public abstract class AbstractCheckableStore<T extends AbstractCheckableStore<T>> extends PartialStore<T> implements CheckableStore<T> {

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
	private final io.hotmoka.xodus.env.Store storeOfHistories;

	/**
	 * The root of the trie of the errors. It is empty if the trie is empty.
	 */
	private Optional<byte[]> rootOfErrors = Optional.empty();

	/**
	 * The root of the trie of the requests. It is empty if the trie is empty.
	 */
	private Optional<byte[]> rootOfRequests = Optional.empty();

	/**
	 * The root of the trie of histories. It is empty if the trie is empty.
	 */
	private Optional<byte[]> rootOfHistories = Optional.empty();

	/**
     * The trie of the errors.
     */
	private TrieOfErrors trieOfErrors;

	/**
     * The trie of the requests.
     */
	protected TrieOfRequests trieOfRequests;

	/**
	 * The trie of histories.
	 */
	private TrieOfHistories trieOfHistories;

	/**
     * Creates the store. Its roots are not yet initialized. Hence, after this constructor,
	 * a call to {@link #setRootsTo(byte[])} should occur, to set the roots of the store.
     * 
     * @param dir the path where the database of the store gets created
     * @param checkableDepth the number of last commits that can be checked out, in order to
	 *                       change the world-view of the store (see {@link #checkoutAt(byte[])}).
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
	protected AbstractCheckableStore(Path dir) {
		this(new Roots(dir));
	}

	protected AbstractCheckableStore(Roots roots) {
		super(roots);

		var storeOfErrors = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfRequests = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfHistories = new AtomicReference<io.hotmoka.xodus.env.Store>();

		env.executeInTransaction(txn -> {
			storeOfErrors.set(env.openStoreWithoutDuplicates("errors", txn));
			storeOfRequests.set(env.openStoreWithoutDuplicates("requests", txn));
			storeOfHistories.set(env.openStoreWithoutDuplicates("history", txn));
		});

		this.storeOfErrors = storeOfErrors.get();
		this.storeOfRequests = storeOfRequests.get();
		this.storeOfHistories = storeOfHistories.get();

		Optional<byte[]> hashesOfRoots = roots.get();

    	if (hashesOfRoots.isEmpty()) {
    		rootOfErrors = Optional.empty();
    		rootOfRequests = Optional.empty();
    		rootOfHistories = Optional.empty();
    	}
    	else {
    		var rootOfErrors = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 64, rootOfErrors, 0, 32);
    		this.rootOfErrors = Optional.of(rootOfErrors);

    		var rootOfRequests = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 96, rootOfRequests, 0, 32);
    		this.rootOfRequests = Optional.of(rootOfRequests);

    		var rootOfHistory = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 128, rootOfHistory, 0, 32);
    		this.rootOfHistories = Optional.of(rootOfHistory);
    	}
	}

	protected AbstractCheckableStore(AbstractCheckableStore<T> toClone) {
		super(toClone);

		this.storeOfErrors = toClone.storeOfErrors;
		this.storeOfHistories = toClone.storeOfHistories;
		this.storeOfRequests = toClone.storeOfRequests;

		synchronized (toClone.lock) {
			this.rootOfErrors = toClone.rootOfErrors;
			this.rootOfHistories = toClone.rootOfHistories;
			this.rootOfRequests = toClone.rootOfRequests;
			this.trieOfErrors = toClone.trieOfErrors;
			this.trieOfHistories = toClone.trieOfHistories;
			this.trieOfRequests = toClone.trieOfRequests;
		}
	}

	protected AbstractCheckableStore(AbstractCheckableStore<T> toClone, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo) {
    	this(toClone, rootOfResponses, rootOfInfo, toClone.rootOfErrors, toClone.rootOfHistories, toClone.rootOfRequests);
    }

	protected AbstractCheckableStore(AbstractCheckableStore<T> toClone, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests) {
    	super(toClone, rootOfResponses, rootOfInfo);

    	this.storeOfErrors = toClone.storeOfErrors;
		this.storeOfHistories = toClone.storeOfHistories;
		this.storeOfRequests = toClone.storeOfRequests;

    	synchronized (toClone.lock) {
    		this.rootOfErrors = rootOfErrors;
			this.rootOfHistories = rootOfHistories;
			this.rootOfRequests = rootOfRequests;
			this.trieOfErrors = toClone.trieOfErrors;
			this.trieOfHistories = toClone.trieOfHistories;
			this.trieOfRequests = toClone.trieOfRequests;
		}
    }

	@Override
	public Optional<String> getError(TransactionReference reference) throws StoreException {
    	synchronized (lock) {
    		try {
				return CheckSupplier.check(TrieException.class, () -> env.computeInReadonlyTransaction
					(UncheckFunction.uncheck(txn -> new TrieOfErrors(new KeyValueStoreOnXodus(storeOfErrors, txn), rootOfErrors).get(reference))));
			}
    		catch (TrieException e) {
    			throw new StoreException(e);
			}
    	}
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		synchronized (lock) {
			return env.computeInReadonlyTransaction // TODO: recheck
				(UncheckFunction.uncheck(txn -> new TrieOfRequests(new KeyValueStoreOnXodus(storeOfRequests, txn), rootOfRequests).get(reference)));
		}
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) throws StoreException {
		try {
			synchronized (lock) {
				return CheckSupplier.check(TrieException.class, () -> env.computeInReadonlyTransaction
						(UncheckFunction.uncheck(txn -> new TrieOfHistories(new KeyValueStoreOnXodus(storeOfHistories, txn), rootOfHistories).get(object))).orElse(Stream.empty()));
			}
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Stream<TransactionReference> getHistoryUncommitted(StorageReference object) throws StoreException {
		synchronized (lock) {
			try {
				return duringTransaction() ? trieOfHistories.get(object).orElse(Stream.empty()) : getHistory(object);
			}
			catch (TrieException e) {
				throw new StoreException(e);
			}
		}
	}

	@Override
	public void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) throws StoreException {
		try {
			synchronized (lock) {
				trieOfRequests = trieOfRequests.put(reference, request);
				trieOfErrors = trieOfErrors.put(reference, errorMessage);
			}
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Transaction beginTransaction() {
		synchronized (lock) {
			Transaction txn = super.beginTransaction();

			try {
				trieOfErrors = new TrieOfErrors(new KeyValueStoreOnXodus(storeOfErrors, txn), rootOfErrors);
				trieOfRequests = new TrieOfRequests(new KeyValueStoreOnXodus(storeOfRequests, txn), rootOfRequests);
				trieOfHistories = new TrieOfHistories(new KeyValueStoreOnXodus(storeOfHistories, txn), rootOfHistories);
			}
			catch (TrieException e) {
				throw new RuntimeException(e); // TODO
			}

			return txn;
		}
	}

	/*@Override
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
	}*/

	@Override
	protected T setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws StoreException {
		T result = super.setResponse(reference, request, response);
	
		// we also store the request
		try {
			result.trieOfRequests = result.trieOfRequests.put(reference, request);
			return result.mkClone();
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected T setHistory(StorageReference object, Stream<TransactionReference> history) {
		try {
			trieOfHistories = trieOfHistories.put(object, history);
			return mkClone();
		}
		catch (TrieException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	@Override
	protected byte[] mergeRootsOfTries() throws StoreException {
		byte[] superMerge = super.mergeRootsOfTries();
		byte[] result = new byte[superMerge.length + 96];
		System.arraycopy(superMerge, 0, result, 0, superMerge.length);
		System.arraycopy(trieOfErrors.getRoot(), 0, result, superMerge.length, 32);
		System.arraycopy(trieOfRequests.getRoot(), 0, result, superMerge.length + 32, 32);
		System.arraycopy(trieOfHistories.getRoot(), 0, result, superMerge.length + 64, 32);

		return result;
	}

	@Override
	public byte[] getStateId() throws StoreException {
		return mergeRootsOfTries();
	}

	@Override
	protected void setRootsTo(byte[] root) {
		super.setRootsTo(root);

		var bytesOfRootOfErrors = new byte[32];
		System.arraycopy(root, 64, bytesOfRootOfErrors, 0, 32);
		rootOfErrors = Optional.of(bytesOfRootOfErrors);

		var bytesOfRootOfRequests = new byte[32];
		System.arraycopy(root, 96, bytesOfRootOfRequests, 0, 32);
		rootOfRequests = Optional.of(bytesOfRootOfRequests);

		var bytesOfRootOfHistories = new byte[32];
		System.arraycopy(root, 128, bytesOfRootOfHistories, 0, 32);
		rootOfHistories = Optional.of(bytesOfRootOfHistories);
	}

	@Override
	protected boolean isEmpty() {
		return super.isEmpty() && rootOfErrors.isEmpty() && rootOfRequests.isEmpty() && rootOfHistories.isEmpty();
	}
}