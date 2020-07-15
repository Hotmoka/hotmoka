package io.hotmoka.stores;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.stores.internal.TrieOfErrors;
import io.hotmoka.stores.internal.TrieOfRequests;
import io.takamaka.code.engine.AbstractNode;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions, together with their
 * requests or errors (for this reason it is <i>full</i>).
 * This store has the ability of changing its <i>world view</i> by checking out different
 * hashes of its root. Hence, it can be used to come back in time or change
 * history branch by simply checking out a different root. Its implementation
 * is based on Merkle-Patricia tries, supported by JetBrains' Xodus transactional database.
 * 
 * The information kept in this store consists of:
 * 
 * <ul>
 * <li> a map from each Hotmoka request reference to the response computed for that request
 * <li> a map from each storage reference to the transaction references that contribute
 *      to provide values to the fields of the storage object at that reference
 *      (this is used by a node to reconstruct the state of the objects in store)
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current number of commits
 * <li> a map from each Hotmoka request reference to the corresponding request
 * <li> a map from each Hotmoka request reference to the error that its execution generated
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 */
public class FullTrieBasedStore<N extends AbstractNode<?,?>> extends PartialTrieBasedStore<N> {

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the errors of the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfErrors;

	/**
	 * The Xodus store that holds the Merkle-Patricia trie of the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfRequests;

	/**
	 * The root of the trie of the errors. It is an empty array if the trie is empty.
	 */
	private byte[] rootOfErrors;

	/**
	 * The root of the trie of the requests. It is an empty array if the trie is empty.
	 */
	private byte[] rootOfRequests;

	/**
     * The trie of the errors.
     */
	private TrieOfErrors trieOfErrors;

	/**
     * The trie of the requests.
     */
	private TrieOfRequests trieOfRequests;

	/**
     * Creates the store initialized to the view of the last checked out root.
     * 
     * @param node the node for which the store is being built
     */
	protected FullTrieBasedStore(N node) {
		super(node);

		try {
			AtomicReference<io.hotmoka.xodus.env.Store> storeOfErrors = new AtomicReference<>();
			AtomicReference<io.hotmoka.xodus.env.Store> storeOfRequests = new AtomicReference<>();

			recordTime(() -> env.executeInTransaction(txn -> {
				storeOfErrors.set(env.openStoreWithoutDuplicates("errors", txn));
				storeOfRequests.set(env.openStoreWithoutDuplicates("requests", txn));
			}));

			this.storeOfErrors = storeOfErrors.get();
			this.storeOfRequests = storeOfRequests.get();
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

    /**
     * Creates a store initialized to the view of the given root.
     * 
	 * @param node the node for which the store is being built
     * @param hash the root to use for the store
     */
    protected FullTrieBasedStore(N node, byte[] hash) {
    	super(node, hash);

    	try {
			AtomicReference<io.hotmoka.xodus.env.Store> storeOfErrors = new AtomicReference<>();
			AtomicReference<io.hotmoka.xodus.env.Store> storeOfRequests = new AtomicReference<>();

			recordTime(() -> env.executeInTransaction(txn -> {
				storeOfErrors.set(env.openStoreWithoutDuplicates("errors", txn));
				storeOfRequests.set(env.openStoreWithoutDuplicates("requests", txn));
			}));

			this.storeOfErrors = storeOfErrors.get();
			this.storeOfRequests = storeOfRequests.get();
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
    }

    @Override
	public final Optional<String> getError(TransactionReference reference) {
    	return recordTime(() -> env.computeInReadonlyTransaction(txn -> new TrieOfErrors(storeOfErrors, txn, nullIfEmpty(rootOfErrors)).get(reference)));
	}

	@Override
	public final Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> new TrieOfRequests(storeOfRequests, txn, nullIfEmpty(rootOfRequests)).get(reference)));
	}

	@Override
	public final void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		recordTime(() -> trieOfRequests.put(reference, request));
		recordTime(() -> trieOfErrors.put(reference, errorMessage));
	}

	@Override
	public void beginTransaction(long now) {
		super.beginTransaction(now);

		trieOfErrors = new TrieOfErrors(storeOfErrors, txn, nullIfEmpty(rootOfErrors));
		trieOfRequests = new TrieOfRequests(storeOfRequests, txn, nullIfEmpty(rootOfRequests));
	}

	@Override
	protected void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		super.setResponse(reference, request, response);
	
		// we also store the request
		recordTime(() -> trieOfRequests.put(reference, request));
	}

	@Override
	protected byte[] mergeRootsOfTries() {
		byte[] superMerge = super.mergeRootsOfTries();
		byte[] result = new byte[superMerge.length + 64];
		System.arraycopy(superMerge, 0, result, 0, superMerge.length);

		byte[] rootOfErrors = trieOfErrors.getRoot();
		if (rootOfErrors != null)
			System.arraycopy(rootOfErrors, 0, result, 64, 32);

		byte[] rootOfRequests = trieOfRequests.getRoot();
		if (rootOfRequests != null)
			System.arraycopy(rootOfRequests, 0, result, 96, 32);

		return result;
	}

	@Override
	protected byte[] mergeCurrentRoots() {
		byte[] superMerge = super.mergeCurrentRoots();
		byte[] result = new byte[superMerge.length + 64];
		System.arraycopy(superMerge, 0, result, 0, superMerge.length);
		System.arraycopy(rootOfErrors, 0, result, 64, 32);
		System.arraycopy(rootOfRequests, 0, result, 96, 32);

		return result;
	}

	@Override
	protected void splitRoots(byte[] root) {
		super.splitRoots(root);

		if (rootOfErrors == null)
			rootOfErrors = new byte[32];

		if (rootOfRequests == null)
			rootOfRequests = new byte[32];

		if (root == null) {
			Arrays.fill(rootOfErrors, (byte) 0);
			Arrays.fill(rootOfRequests, (byte) 0);
		}
		else {
			System.arraycopy(root, 64, rootOfErrors, 0, 32);
			System.arraycopy(root, 96, rootOfRequests, 0, 32);
		}
	}

	@Override
	protected boolean isEmpty() {
		if (!super.isEmpty())
			return false;

		for (byte b: rootOfErrors)
			if (b != (byte) 0)
				return false;

		for (byte b: rootOfRequests)
			if (b != (byte) 0)
				return false;

		return true;
	}
}