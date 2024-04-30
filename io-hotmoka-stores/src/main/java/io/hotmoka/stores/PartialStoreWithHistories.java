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
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.stores.internal.KeyValueStoreOnXodus;
import io.hotmoka.stores.internal.TrieOfHistories;
import io.hotmoka.xodus.env.Transaction;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions, together with their
 * histories (for this reason it is <i>with history</i>).
 * This store has the ability of changing its <i>world view</i> by checking out different
 * hashes of its roots. Hence, it can be used to come back in time or change
 * history branch or create a snapshot of it by simply checking out a different root. Its implementation
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
 *      is installed or the current root and number of commits
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 */
@ThreadSafe
public abstract class PartialStoreWithHistories<T extends PartialStoreWithHistories<T>> extends PartialStore<T> {

	/**
	 * The Xodus store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final io.hotmoka.xodus.env.Store storeOfHistories;

	/**
	 * The root of the trie of histories. It is empty if the trie is empty.
	 */
	private Optional<byte[]> rootOfHistories;

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
	protected PartialStoreWithHistories(Path dir) {
		this(new Roots(dir));
	}

	protected PartialStoreWithHistories(Roots roots) {
		super(roots);

		Optional<byte[]> hashesOfRoots = roots.get();

    	AtomicReference<io.hotmoka.xodus.env.Store> storeOfHistories = new AtomicReference<>();
		roots.getEnvironment().executeInTransaction(txn -> storeOfHistories.set(env.openStoreWithoutDuplicates("history", txn)));
		this.storeOfHistories = storeOfHistories.get();

		if (hashesOfRoots.isEmpty())
    		rootOfHistories = Optional.empty();
    	else {
    		var rootOfHistory = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 64, rootOfHistory, 0, 32);
    		this.rootOfHistories = Optional.of(rootOfHistory);
    	}
	}

	protected PartialStoreWithHistories(PartialStoreWithHistories<T> toClone) {
		super(toClone);

		this.storeOfHistories = toClone.storeOfHistories;

		synchronized (toClone.lock) {
			this.rootOfHistories = toClone.rootOfHistories;
			this.trieOfHistories = toClone.trieOfHistories;
		}
	}

	protected PartialStoreWithHistories(PartialStoreWithHistories<T> toClone, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo) {
    	this(toClone, rootOfResponses, rootOfInfo, toClone.rootOfHistories);
    }

	protected PartialStoreWithHistories(PartialStoreWithHistories<T> toClone, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfHistories) {
    	super(toClone, rootOfResponses, rootOfInfo);

    	this.storeOfHistories = toClone.storeOfHistories;

    	synchronized (toClone.lock) {
			this.rootOfHistories = rootOfHistories;
			this.trieOfHistories = toClone.trieOfHistories;
		}
    }

	protected abstract T mkClone(Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfHistories);

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
	public Transaction beginTransaction() {
		synchronized (lock) {
			Transaction txn = super.beginTransaction();

			try {
				trieOfHistories = new TrieOfHistories(new KeyValueStoreOnXodus(storeOfHistories, txn), rootOfHistories);
			}
			catch (TrieException e) {
				throw new RuntimeException(e);
			}

			return txn;
		}
	}

	/*@Override
	protected void garbageCollect(long commitNumber) throws StoreException {
		super.garbageCollect(commitNumber);

		try {
			trieOfHistories.garbageCollect(commitNumber);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}*/

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
		var result = new byte[superMerge.length + 32];
		System.arraycopy(superMerge, 0, result, 0, superMerge.length);
		System.arraycopy(trieOfHistories.getRoot(), 0, result, superMerge.length, 32);

		return result;
	}

	@Override
	protected void setRootsTo(byte[] root) {
		super.setRootsTo(root);

		var bytesOfRootOfHistories = new byte[32];
		System.arraycopy(root, 64, bytesOfRootOfHistories, 0, 32);
		rootOfHistories = Optional.of(bytesOfRootOfHistories);
	}

	@Override
	protected boolean isEmpty() {
		return super.isEmpty() && rootOfHistories.isEmpty();
	}
}