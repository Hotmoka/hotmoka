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
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.stores.internal.KeyValueStoreOnXodus;
import io.hotmoka.stores.internal.TrieOfInfo;
import io.hotmoka.stores.internal.TrieOfResponses;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions
 * but not their requests, histories and errors (for this reason it is <i>partial</i>).
 * Its implementation is based on Merkle-Patricia tries,
 * supported by JetBrains' Xodus transactional database.
 * 
 * The information kept in this store consists of:
 * 
 * <ul>
 * <li> a map from each Hotmoka request reference to the response computed for that request
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current root and number of commits
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 * 
 * This class is meant to be subclassed by specifying where errors, requests and histories are kept.
 */
@ThreadSafe
public abstract class PartialStore<T extends PartialStore<T>> extends AbstractStore<T> {

	/**
	 * The Xodus environment that holds the store.
	 */
	protected final Environment env;

	/**
	 * The number of last commits that can be checked out, in order to
	 * change the world-view of the store (see {@link #checkoutAt(byte[])}).
	 * This entails that such commits are not garbage-collected. until
	 * new commits get created on top and they end up being deeper.
	 * This is useful if we expect an old state to be checked out, for
	 * instance because a blockchain swaps to another history, but we can
	 * assume a reasonable depth for that to happen. Use 0 if the store
	 * is not checkable, so that all its successive commits can be immediately
	 * garbage-collected as soon as a new commit is created on top
	 * (which corresponds to a blockchain that never swaps to a previous
	 * state, because it has deterministic finality).
	 */
	private final long checkableDepth;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfResponses;

	/**
	 * The Xodus store that holds miscellaneous information about the store.
	 */
    private final io.hotmoka.xodus.env.Store storeOfInfo;

	/**
	 * The root of the trie of the responses. It is empty if the trie is empty.
	 */
	private Optional<byte[]> rootOfResponses;

	/**
	 * The root of the trie of the miscellaneous info. It is empty if the trie is empty.
	 */
	private Optional<byte[]> rootOfInfo;

	/**
	 * The key used inside {@link #storeOfInfo} to keep the root.
	 */
	private final static ByteIterable ROOT = ByteIterable.fromBytes("root".getBytes());

	/**
	 * The transaction that accumulates all changes to commit.
	 */
	private Transaction txn;

	/**
     * The trie of the responses.
     */
	private TrieOfResponses trieOfResponses;

	/**
	 * The trie for the miscellaneous information.
	 */
	private TrieOfInfo trieOfInfo;

	private final static Logger logger = Logger.getLogger(PartialStore.class.getName());

	/**
	 * Creates a store. Its roots are not yet initialized. Hence, after this constructor,
	 * a call to {@link #setRootsTo(Optional)} should occur, to set the roots of the store.
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
    protected PartialStore(Path dir, long checkableDepth) {
    	this(checkableDepth, new Roots(dir));
    }

    protected PartialStore(long checkableDepth, Roots roots) {
    	this.checkableDepth = checkableDepth;
    	this.env = roots.env;

    	var storeOfResponses = new AtomicReference<io.hotmoka.xodus.env.Store>();
    	env.executeInTransaction(txn -> storeOfResponses.set(env.openStoreWithoutDuplicates("responses", txn)));

    	this.storeOfResponses = storeOfResponses.get();
    	this.storeOfInfo = roots.storeOfInfo;

    	Optional<byte[]> hashesOfRoots = roots.get();

    	if (hashesOfRoots.isEmpty()) {
    		rootOfResponses = Optional.empty();
    		rootOfInfo = Optional.empty();
    	}
    	else {
    		var rootOfResponses = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 0, rootOfResponses, 0, 32);
    		this.rootOfResponses = Optional.of(rootOfResponses);

    		var rootOfInfo = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 32, rootOfInfo, 0, 32);
    		this.rootOfInfo = Optional.of(rootOfInfo);
    	}
    }

    protected static class Roots {
    	private final Environment env;
        private final io.hotmoka.xodus.env.Store storeOfInfo;
    	private final Optional<byte[]> roots;

    	protected Roots(Path dir) {
    		this.env = new Environment(dir + "/store");

    		var storeOfInfo = new AtomicReference<io.hotmoka.xodus.env.Store>();
    		var roots = new AtomicReference<Optional<byte[]>>();

    		env.executeInTransaction(txn -> {
    			storeOfInfo.set(env.openStoreWithoutDuplicates("info", txn));
        		roots.set(Optional.ofNullable(storeOfInfo.get().get(txn, ROOT)).map(ByteIterable::getBytes));
        	});

    		this.storeOfInfo = storeOfInfo.get();
    		this.roots = roots.get();
    	}

    	public Optional<byte[]> get() {
    		return roots;
    	}

    	public Environment getEnvironment() {
    		return env;
    	}
    }

    protected PartialStore(PartialStore<T> toClone) {
    	super(toClone);

    	this.env = toClone.env;
    	this.checkableDepth = toClone.checkableDepth;
    	this.storeOfResponses = toClone.storeOfResponses;
    	this.storeOfInfo = toClone.storeOfInfo;

    	synchronized (toClone.lock) {
    		this.rootOfResponses = toClone.rootOfResponses;
    		this.rootOfInfo = toClone.rootOfInfo;
    		this.txn = toClone.txn;
    		this.trieOfResponses = toClone.trieOfResponses;
    		this.trieOfInfo = toClone.trieOfInfo;
    	}
    }

    protected PartialStore(PartialStore<T> toClone, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo) {
    	super(toClone);

    	this.env = toClone.env;
    	this.checkableDepth = toClone.checkableDepth;
    	this.storeOfResponses = toClone.storeOfResponses;
    	this.storeOfInfo = toClone.storeOfInfo;

    	synchronized (toClone.lock) {
    		this.rootOfResponses = rootOfResponses;
    		this.rootOfInfo = rootOfInfo;
    		this.txn = toClone.txn;
    		this.trieOfResponses = toClone.trieOfResponses;
    		this.trieOfInfo = toClone.trieOfInfo;
    	}
    }

    protected abstract T mkClone(Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo);

    @Override
    public void close() {
    	if (duringTransaction()) {
    		// store closed with yet uncommitted transactions: we abort them
    		logger.log(Level.WARNING, "store closed with uncommitted transactions: they are being aborted");
    		txn.abort();
    	}

    	try {
    		env.close();
    	}
    	catch (ExodusException e) {
    		logger.log(Level.WARNING, "failed to close environment", e);
    	}

    	super.close();
    }

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
    	synchronized (lock) {
    		return env.computeInReadonlyTransaction // TODO: recheck
    			(UncheckFunction.uncheck(txn -> new TrieOfResponses(new KeyValueStoreOnXodus(storeOfResponses, txn), rootOfResponses, -1L).get(reference)));
    	}
	}

	@Override
	public Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		synchronized (lock) {
			try {
				return duringTransaction() ? trieOfResponses.get(reference) : getResponse(reference);
			}
			catch (TrieException e) {
				throw new RuntimeException(e); // TODO
			}
		}
	}

	@Override
	public Optional<StorageReference> getManifest() throws StoreException {
		try {
			synchronized (lock) {
				return CheckSupplier.check(TrieException.class, () ->
					env.computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo, -1L).getManifest())));
			}
		}
		catch (ExodusException | TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() throws StoreException {
		try {
			synchronized (lock) {
				return duringTransaction() ? trieOfInfo.getManifest() : getManifest();
			}
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Starts a transaction. Instance updates during the transaction are saved
	 * in the supporting database if the transaction will later be committed.
	 */
	public final void beginTransaction() {
		synchronized (lock) {
			beginTransactionInternal();
		}
	}

	/**
	 * Commits to the database all data written from the last call to {@link #beginTransactionInternal()}.
	 * This does not change the view of the store, since its roots are not updated,
	 * unless the hash returned by this method gets later checked out to update the roots.
	 * 
	 * @return the hash of the store resulting at the end of all updates performed during the transaction;
	 *         if this gets checked out, the view of the store becomes that at the end of the transaction
	 */
	public byte[] commitTransaction() {
		try {
			synchronized (lock) {
				trieOfInfo = trieOfInfo.increaseNumberOfCommits();
				long newCommitNumber = trieOfInfo.getNumberOfCommits();

				// a negative number means that garbage-collection is disabled
				if (checkableDepth >= 0L) {
					long commitToGarbageCollect = newCommitNumber - 1 - checkableDepth;
					if (commitToGarbageCollect >= 0L)
						garbageCollect(commitToGarbageCollect);
				}

				if (!txn.commit())
					logger.info("transaction's commit failed");

				return mergeRootsOfTries();
			}
		}
		catch (StoreException | TrieException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	/**
	 * Yields the number of commits already performed over this store.
	 * 
	 * @return the number of commits
	 */
	public long getNumberOfCommits() {
		return env.computeInReadonlyTransaction // TODO: recheck
			(UncheckFunction.uncheck(txn -> new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo, -1L).getNumberOfCommits()));
	}

	/**
	 * Starts a transaction. Instance updates during the transaction are saved
	 * in the supporting database if the transaction will later be committed.
	 * 
	 * @return the transaction
	 */
	protected Transaction beginTransactionInternal() {
		txn = env.beginTransaction();
		long numberOfCommits = getNumberOfCommits();

		try {
			trieOfResponses = new TrieOfResponses(new KeyValueStoreOnXodus(storeOfResponses, txn), rootOfResponses, numberOfCommits);
			trieOfInfo = new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo, numberOfCommits);
		}
		catch (TrieException e) {
			throw new RuntimeException(e); // TODO
		}

		return txn;
	}

	@Override
	protected T setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws StoreException {
		try {
			TrieOfResponses newTrieOfResponses = trieOfResponses.put(reference, response);
			this.trieOfResponses = newTrieOfResponses;
			return mkClone(); // TODO Optional.of(newTrieOfResponses.getRoot()), rootOfInfo);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected T setManifest(StorageReference manifest) throws StoreException {
		try {
			TrieOfInfo newTrieOfInfo = trieOfInfo.setManifest(manifest);
			this.trieOfInfo = newTrieOfInfo;
			return mkClone(); //rootOfResponses, Optional.of(newTrieOfInfo.getRoot()));
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Garbage-collects all keys updated during the given commit.
	 * 
	 * @param commitNumber the number of the commit
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected void garbageCollect(long commitNumber) throws StoreException {
		try {
			trieOfResponses.garbageCollect(commitNumber);
			trieOfInfo.garbageCollect(commitNumber);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Resets the store to the given root. This is just the concatenation of the roots
	 * of the tries in this store. For instance, as returned by a previous {@link #commitTransaction()}.
	 * 
	 * @param root the root to reset to
	 */
	protected void checkoutAt(byte[] root) {
		setRootsTo(root);
		var rootAsBI = ByteIterable.fromBytes(root);
		env.executeInTransaction(txn -> storeOfInfo.put(txn, ROOT, rootAsBI));
	}

	/**
	 * Yields the Xodus transaction active between {@link #beginTransactionInternal()} and
	 * {@link #commitTransaction()}. This is where store updates must be written.
	 * 
	 * @return the transaction
	 */
	protected final Transaction getCurrentTransaction() {
		return txn;
	}

	/**
	 * Determines if the store is between a {@link #beginTransactionInternal()} and a
	 * {@link #commitTransaction()}.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected final boolean duringTransaction() {
		return txn != null && !txn.isFinished();
	}

	/**
	 * Sets the roots of this store to the given (merged) root.
	 * 
	 * @param root the merged root; this is empty if the store is empty and has consequently no root yet
	 */
	protected void setRootsTo(byte[] root) {
		var bytesOfRootOfResponses = new byte[32];
		System.arraycopy(root, 0, bytesOfRootOfResponses, 0, 32);
		rootOfResponses = Optional.of(bytesOfRootOfResponses);

		var bytesOfRootOfInfo = new byte[32];
		System.arraycopy(root, 32, bytesOfRootOfInfo, 0, 32);
		rootOfInfo = Optional.of(bytesOfRootOfInfo);
	}

	/**
	 * Yields the concatenation of the roots of the tries in this store,
	 * resulting after all updates performed to the store. Hence, they point
	 * to the latest view of the store.
	 * 
	 * @return the concatenation
	 */
	protected byte[] mergeRootsOfTries() throws StoreException {
		// this can be null if this is called before any new transaction has been executed over this store
		if (trieOfResponses == null)
			return env.computeInReadonlyTransaction(txn -> storeOfInfo.get(txn, ROOT).getBytes());

		var result = new byte[64];
		System.arraycopy(trieOfResponses.getRoot(), 0, result, 0, 32);
		System.arraycopy(trieOfInfo.getRoot(), 0, result, 32, 32);

		return result;
	}

	/**
	 * Determines if all roots of the tries in this store are empty.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected boolean isEmpty() {
		return rootOfResponses.isEmpty() && rootOfInfo.isEmpty();
	}
}