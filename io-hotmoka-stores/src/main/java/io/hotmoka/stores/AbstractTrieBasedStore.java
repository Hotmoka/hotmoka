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
import io.hotmoka.stores.internal.TrieOfInfo;
import io.hotmoka.stores.internal.TrieOfRequests;
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
public abstract class AbstractTrieBasedStore<T extends AbstractTrieBasedStore<T>> extends AbstractStore<T> {

	/**
	 * The Xodus environment that holds the store.
	 */
	protected final Environment env;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfResponses;

	/**
	 * The Xodus store that holds miscellaneous information about the store.
	 */
    private final io.hotmoka.xodus.env.Store storeOfInfo;

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
	 * The root of the trie of the responses. It is empty if the trie is empty.
	 */
	private Optional<byte[]> rootOfResponses;

	/**
	 * The root of the trie of the miscellaneous info. It is empty if the trie is empty.
	 */
	private Optional<byte[]> rootOfInfo;

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
	 * The trie of the responses.
	 */
	private TrieOfResponses trieOfResponses;

	/**
	 * The trie for the miscellaneous information.
	 */
	private TrieOfInfo trieOfInfo;

	/**
     * The trie of the errors.
     */
	protected TrieOfErrors trieOfErrors;

	/**
     * The trie of the requests.
     */
	protected TrieOfRequests trieOfRequests;

	/**
	 * The trie of histories.
	 */
	private TrieOfHistories trieOfHistories;

	/**
	 * The key used inside {@link #storeOfInfo} to keep the root.
	 */
	private final static ByteIterable ROOT = ByteIterable.fromBytes("root".getBytes());

	/**
	 * The transaction that accumulates all changes to commit.
	 */
	private Transaction txn;

	private final static Logger LOGGER = Logger.getLogger(AbstractTrieBasedStore.class.getName());

	/**
	 * Creates a store. Its roots are not yet initialized. Hence, after this constructor,
	 * a call to {@link #setRootsTo(Optional)} should occur, to set the roots of the store.
	 * 
 	 * @param dir the path where the database of the store gets created
	 */
    protected AbstractTrieBasedStore(Path dir) {
    	this.env = new Environment(dir + "/store");

		var storeOfInfo = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var roots = new AtomicReference<Optional<byte[]>>();

		env.executeInTransaction(txn -> {
			storeOfInfo.set(env.openStoreWithoutDuplicates("info", txn));
    		roots.set(Optional.ofNullable(storeOfInfo.get().get(txn, ROOT)).map(ByteIterable::getBytes));
    	});

    	var storeOfResponses = new AtomicReference<io.hotmoka.xodus.env.Store>();
    	var storeOfErrors = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfRequests = new AtomicReference<io.hotmoka.xodus.env.Store>();
		var storeOfHistories = new AtomicReference<io.hotmoka.xodus.env.Store>();

		env.executeInTransaction(txn -> {
			storeOfResponses.set(env.openStoreWithoutDuplicates("responses", txn));
			storeOfErrors.set(env.openStoreWithoutDuplicates("errors", txn));
			storeOfRequests.set(env.openStoreWithoutDuplicates("requests", txn));
			storeOfHistories.set(env.openStoreWithoutDuplicates("history", txn));
		});

    	this.storeOfResponses = storeOfResponses.get();
    	this.storeOfInfo = storeOfInfo.get();
    	this.storeOfErrors = storeOfErrors.get();
		this.storeOfRequests = storeOfRequests.get();
		this.storeOfHistories = storeOfHistories.get();

    	Optional<byte[]> hashesOfRoots = roots.get();

    	if (hashesOfRoots.isEmpty()) {
    		rootOfResponses = Optional.empty();
    		rootOfInfo = Optional.empty();
    		rootOfErrors = Optional.empty();
    		rootOfRequests = Optional.empty();
    		rootOfHistories = Optional.empty();
    	}
    	else {
    		var rootOfResponses = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 0, rootOfResponses, 0, 32);
    		this.rootOfResponses = Optional.of(rootOfResponses);

    		var rootOfInfo = new byte[32];
    		System.arraycopy(hashesOfRoots.get(), 32, rootOfInfo, 0, 32);
    		this.rootOfInfo = Optional.of(rootOfInfo);

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

    protected AbstractTrieBasedStore(AbstractTrieBasedStore<T> toClone) {
    	super(toClone);

    	this.env = toClone.env;
    	this.storeOfResponses = toClone.storeOfResponses;
    	this.storeOfInfo = toClone.storeOfInfo;
    	this.storeOfErrors = toClone.storeOfErrors;
		this.storeOfHistories = toClone.storeOfHistories;
		this.storeOfRequests = toClone.storeOfRequests;

    	synchronized (toClone.lock) {
    		this.rootOfResponses = toClone.rootOfResponses;
    		this.rootOfInfo = toClone.rootOfInfo;
    		this.rootOfErrors = toClone.rootOfErrors;
			this.rootOfHistories = toClone.rootOfHistories;
			this.rootOfRequests = toClone.rootOfRequests;
    		this.trieOfResponses = toClone.trieOfResponses;
    		this.trieOfInfo = toClone.trieOfInfo;
			this.trieOfErrors = toClone.trieOfErrors;
			this.trieOfHistories = toClone.trieOfHistories;
			this.trieOfRequests = toClone.trieOfRequests;
			this.txn = toClone.txn;
		}
    }

    protected AbstractTrieBasedStore(AbstractTrieBasedStore<T> toClone, Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests) {
    	super(toClone);

    	this.env = toClone.env;
    	this.storeOfResponses = toClone.storeOfResponses;
    	this.storeOfInfo = toClone.storeOfInfo;
    	this.storeOfErrors = toClone.storeOfErrors;
		this.storeOfHistories = toClone.storeOfHistories;
		this.storeOfRequests = toClone.storeOfRequests;

    	synchronized (toClone.lock) {
    		this.rootOfResponses = rootOfResponses;
    		this.rootOfInfo = rootOfInfo;
    		this.rootOfErrors = rootOfErrors;
			this.rootOfHistories = rootOfHistories;
			this.rootOfRequests = rootOfRequests;
    		this.trieOfResponses = toClone.trieOfResponses;
    		this.trieOfInfo = toClone.trieOfInfo;
			this.trieOfErrors = toClone.trieOfErrors;
			this.trieOfHistories = toClone.trieOfHistories;
			this.trieOfRequests = toClone.trieOfRequests;
			this.txn = toClone.txn;
		}
    }

    protected abstract T mkClone(Optional<byte[]> rootOfResponses, Optional<byte[]> rootOfInfo, Optional<byte[]> rootOfErrors, Optional<byte[]> rootOfHistories, Optional<byte[]> rootOfRequests);

    @Override
    public void close() {
    	if (duringTransaction()) {
    		// store closed with yet uncommitted transactions: we abort them
    		LOGGER.log(Level.WARNING, "store closed with uncommitted transactions: they are being aborted");
    		txn.abort();
    	}

    	try {
    		env.close();
    	}
    	catch (ExodusException e) {
    		LOGGER.log(Level.WARNING, "failed to close environment", e);
    	}

    	super.close();
    }

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
    	synchronized (lock) {
    		return env.computeInReadonlyTransaction // TODO: recheck
    			(UncheckFunction.uncheck(txn -> new TrieOfResponses(new KeyValueStoreOnXodus(storeOfResponses, txn), rootOfResponses).get(reference)));
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
					env.computeInReadonlyTransaction(UncheckFunction.uncheck(txn -> new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo).getManifest())));
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

	/**
	 * Starts a transaction. Instance updates during the transaction are saved
	 * in the supporting database if the transaction will later be committed.
	 */
	public Transaction beginTransaction() {
		synchronized (lock) {
			txn = env.beginTransaction();

			try {
				trieOfResponses = new TrieOfResponses(new KeyValueStoreOnXodus(storeOfResponses, txn), rootOfResponses);
				trieOfInfo = new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo);
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

	/**
	 * Commits to the database all data written from the last call to {@link #beginTransactionInternal()}.
	 * This does not change the view of the store, since its roots are not updated,
	 * unless the hash returned by this method gets later checked out to update the roots.
	 * 
	 * @return the hash of the store resulting at the end of all updates performed during the transaction;
	 *         if this gets checked out, the view of the store becomes that at the end of the transaction
	 */
	public final T endTransaction() {
		try {
			synchronized (lock) {
				trieOfInfo = trieOfInfo.increaseNumberOfCommits();
				//long newCommitNumber = trieOfInfo.getNumberOfCommits();

				// a negative number means that garbage-collection is disabled
				/*if (checkableDepth >= 0L) {
					long commitToGarbageCollect = newCommitNumber - 1 - checkableDepth;
					if (commitToGarbageCollect >= 0L)
						garbageCollect(commitToGarbageCollect);
				}*/

				if (!txn.commit())
					LOGGER.info("transaction's commit failed");

				T result = mkClone();
				result.setRootsTo(result.mergeRootsOfTries());
				result.moveRootBranchToThis();

				return result;
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
			(UncheckFunction.uncheck(txn -> new TrieOfInfo(new KeyValueStoreOnXodus(storeOfInfo, txn), rootOfInfo).getNumberOfCommits()));
	}

	public byte[] getStateId() throws StoreException {
		return mergeRootsOfTries();
	}

	@Override
	protected T setResponse(TransactionReference reference, TransactionResponse response) throws StoreException {
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

	@Override
	protected T setRequest(TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		try {
			T result = getThis();
			result.trieOfRequests = result.trieOfRequests.put(reference, request);
			return result.mkClone();
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected T setError(TransactionReference reference, String error) throws StoreException {
		try {
			T result = getThis();
			result.trieOfErrors = result.trieOfErrors.put(reference, error);
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

	/**
	 * Garbage-collects all keys updated during the given commit.
	 * 
	 * @param commitNumber the number of the commit
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	/*protected void garbageCollect(long commitNumber) throws StoreException {
		try {
			trieOfResponses.garbageCollect(commitNumber);
			trieOfInfo.garbageCollect(commitNumber);
		}
		catch (TrieException e) {
			throw new StoreException(e);
		}
	}*/

	/**
	 * Resets the store to the given root. This is just the concatenation of the roots
	 * of the tries in this store. For instance, as returned by a previous {@link #commitTransaction()}.
	 * 
	 * @param root the root to reset to
	 */
	public void checkoutAt(byte[] root) {
		synchronized (lock) {
			setRootsTo(root);
		}
	}

	public void moveRootBranchToThis() throws StoreException {
		synchronized (lock) {
			byte[] root = mergeRootsOfTries();
			var rootAsBI = ByteIterable.fromBytes(root);
			env.executeInTransaction(txn -> storeOfInfo.put(txn, ROOT, rootAsBI));
		}
	}

	/**
	 * Determines if the store is between a {@link #beginTransactionInternal()} and a
	 * {@link #endTransaction()}.
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

	/**
	 * Yields the concatenation of the roots of the tries in this store,
	 * resulting after all updates performed to the store. Hence, they point
	 * to the latest view of the store.
	 * 
	 * @return the concatenation
	 */
	protected byte[] mergeRootsOfTries() throws StoreException {
		var result = new byte[160];
		System.arraycopy(trieOfResponses.getRoot(), 0, result, 0, 32);
		System.arraycopy(trieOfInfo.getRoot(), 0, result, 32, 32);
		System.arraycopy(trieOfErrors.getRoot(), 0, result, 64, 32);
		System.arraycopy(trieOfRequests.getRoot(), 0, result, 96, 32);
		System.arraycopy(trieOfHistories.getRoot(), 0, result, 128, 32);

		return result;
	}

	/**
	 * Determines if all roots of the tries in this store are empty.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected boolean isEmpty() {
		return rootOfResponses.isEmpty() && rootOfInfo.isEmpty() && rootOfErrors.isEmpty() && rootOfRequests.isEmpty() && rootOfHistories.isEmpty();
	}
}