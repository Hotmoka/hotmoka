package io.hotmoka.stores;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.local.CheckableStore;
import io.hotmoka.local.Config;
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
public abstract class FullTrieBasedStore<C extends Config> extends PartialTrieBasedStore<C> implements CheckableStore {

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
     * @param config the configuration of the node for which the store is being built
     */
	protected FullTrieBasedStore(C config) {
		super(config);

		try {
			AtomicReference<io.hotmoka.xodus.env.Store> storeOfErrors = new AtomicReference<>();
			AtomicReference<io.hotmoka.xodus.env.Store> storeOfRequests = new AtomicReference<>();
			AtomicReference<io.hotmoka.xodus.env.Store> storeOfHistory = new AtomicReference<>();

			recordTime(() -> env.executeInTransaction(txn -> {
				storeOfErrors.set(env.openStoreWithoutDuplicates("errors", txn));
				storeOfRequests.set(env.openStoreWithoutDuplicates("requests", txn));
				storeOfHistory.set(env.openStoreWithoutDuplicates("history", txn));
			}));

			this.storeOfErrors = storeOfErrors.get();
			this.storeOfRequests = storeOfRequests.get();
			this.storeOfHistory = storeOfHistory.get();
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Builds a clone of the given store.
	 * 
	 * @param parent the store to clone
	 */
	protected FullTrieBasedStore(FullTrieBasedStore<? extends C> parent) {
		super(parent);

		this.storeOfErrors = parent.storeOfErrors;
		this.storeOfRequests = parent.storeOfRequests;
		this.storeOfHistory = parent.storeOfHistory;
		System.arraycopy(parent.rootOfErrors, 0, this.rootOfErrors, 0, 32);
		System.arraycopy(parent.rootOfRequests, 0, this.rootOfRequests, 0, 32);
		System.arraycopy(parent.rootOfHistories, 0, this.rootOfHistories, 0, 32);
	}

    @Override
	public Optional<String> getError(TransactionReference reference) {
    	return recordTimeSynchronized(() -> env.computeInReadonlyTransaction(txn -> new TrieOfErrors(storeOfErrors, txn, nullIfEmpty(rootOfErrors)).get(reference)));
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		return recordTimeSynchronized(() -> env.computeInReadonlyTransaction(txn -> new TrieOfRequests(storeOfRequests, txn, nullIfEmpty(rootOfRequests)).get(reference)));
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) {
		return recordTimeSynchronized(() -> env.computeInReadonlyTransaction(txn -> new TrieOfHistories(storeOfHistory, txn, nullIfEmpty(rootOfHistories)).get(object)));
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
			recordTime(() -> trieOfRequests.put(reference, request));
			recordTime(() -> trieOfErrors.put(reference, errorMessage));
		}
	}

	@Override
	public void beginTransaction(long now) {
		synchronized (lock) {
			super.beginTransaction(now);

			Transaction txn = getCurrentTransaction();
			trieOfErrors = new TrieOfErrors(storeOfErrors, txn, nullIfEmpty(rootOfErrors));
			trieOfRequests = new TrieOfRequests(storeOfRequests, txn, nullIfEmpty(rootOfRequests));
			trieOfHistories = new TrieOfHistories(storeOfHistory, txn, nullIfEmpty(rootOfHistories));
		}
	}

	@Override
	public byte[] commitTransaction() {
		synchronized (lock) {
			return super.commitTransaction();
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
		recordTime(() -> trieOfRequests.put(reference, request));
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		trieOfHistories.put(object, history);
	}

	@Override
	protected byte[] mergeRootsOfTries() {
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