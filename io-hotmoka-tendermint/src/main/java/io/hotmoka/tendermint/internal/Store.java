package io.hotmoka.tendermint.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;

/**
 * A historical store of a node. It is a transactional database that keeps
 * information about the state of the objects created by the requests executed
 * by the node. This state is external to the node and, typically, only
 * its hash is stored in the node, if consensus is needed. This state has
 * the ability of changing its <i>world view</i> by checking out different
 * hashes of its root. Hence, it can be used to come back in time or change
 * history branch by simply checking out a different root. Its implementation
 * is based on Merkle-Patricia tries, supported by JetBrains' Xodus transactional
 * database.
 * 
 * The information kept in this state consists of:
 * 
 * <ul>
 * <li> a map from each Hotmoka request reference to the response computed for that request
 * <li> a map from each storage reference to the transaction references that contribute
 *      to provide values to the fields of the storage object at that reference
 *      (this is used by a node to reconstruct the state of the objects in store)
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current number of commits
 * </ul>
 * 
 * This information is added in store by put methods and accessed through get methods.
 */
class Store extends io.takamaka.code.engine.Store<TendermintBlockchainImpl> {

	/**
	 * The Xodus environment that holds the state.
	 */
	private final Environment env;

	/**
	 * The store that holds the root of the state.
	 */
    private final io.hotmoka.xodus.env.Store storeOfRoot;

    /**
	 * The store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfResponses;

	/**
	 * The store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final io.hotmoka.xodus.env.Store storeOfHistory;

	/**
	 * The store that holds miscellaneous information about the state.
	 */
    private final io.hotmoka.xodus.env.Store storeOfInfo;

	/**
     * The key used inside {@linkplain storeOfRoot} to keep the root.
     */
    private final static ByteIterable ROOT = ByteIterable.fromByte((byte) 0);

    /**
	 * The root of the trie of the responses. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfResponses = new byte[32];

	/**
	 * The root of the trie of the miscellaneous info. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfInfo = new byte[32];

	/**
	 * The transaction that accumulates all changes from begin of block to commit of block.
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

	/**
     * Creates a state that gets persisted inside the given directory.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node the node for which the state is being built
     * @param dir the directory where the state is persisted
     */
    Store(TendermintBlockchainImpl node, String dir) {
    	this(node, dir, (storeOfRoot, txn) -> {
    		ByteIterable root = storeOfRoot.get(txn, ROOT);
    		return root == null ? new byte[64] : root.getBytes();
    	});
    }

    /**
     * Creates a state that gets persisted inside the given directory.
     * It is initialized to the view of the given root.
     * 
	 * @param node the node for which the state is being built
     * @param dir the directory where the state is persisted
     * @param hash the root to use for the state
     */
    Store(TendermintBlockchainImpl node, String dir, byte[] hash) {
    	this(node, dir, (_storeOfRoot, _txn) -> hash);
    }

    @Override
    public void close() {
    	if (txn != null && !txn.isFinished()) {
    		// blockchain closed with yet uncommitted transactions: we abort them
    		logger.error("State closed with uncommitted transactions: they are being aborted");
    		txn.abort();
    	}

    	try {
    		env.close();
    	}
    	catch (ExodusException e) {
    		logger.error("Failed to close environment", e);
    	}

    	super.close();
    }

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> new TrieOfResponses(storeOfResponses, txn, nullIfEmpty(rootOfResponses)).get(reference)));
	}

	@Override
	public Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		// this method uses the last updates to the state, possibly also consequence of
		// transactions inside the current block; however, it might be called also when
		// a block has been committed and the next is not yet started; this can occur for
		// runView transaction methods, that are not transactions inside blocks.
		// Moreover, txn might be null if the first transaction that gets executed
		// with a state is a runView transaction and no new node has been created yet.
		// This might happen for instance upon node recreation
		if (txn == null || txn.isFinished())
			return getResponse(reference);
		else
			return recordTime(() -> trieOfResponses.get(reference));
	}

	@Override
	public Optional<String> getError(TransactionReference reference) {
		try {
			return node.tendermint.getErrorMessage(reference.getHash());
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) {
		return recordTime(() -> {
			ByteIterable historyAsByteArray = env.computeInReadonlyTransaction(txn -> storeOfHistory.get(txn, intoByteArray(object)));
			return historyAsByteArray == null ? Stream.empty() : Stream.of(fromByteArray(TransactionReference::from, TransactionReference[]::new, historyAsByteArray));
		});
	}

	@Override
	public Stream<TransactionReference> getHistoryUncommitted(StorageReference object) {
		if (txn == null || txn.isFinished())
			return getHistory(object);
		else {
			ByteIterable historyAsByteArray = storeOfHistory.get(txn, intoByteArray(object));
			return historyAsByteArray == null ? Stream.empty() : Stream.of(fromByteArray(TransactionReference::from, TransactionReference[]::new, historyAsByteArray));
		}
	}

	@Override
	public Optional<StorageReference> getManifest() {
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo)).getManifest()));
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() {
		if (txn == null || txn.isFinished())
			return getManifest();
		else
			return trieOfInfo.getManifest();
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		try {
			return node.tendermint.getRequest(reference.getHash());
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		recordTime(() -> trieOfResponses.put(reference, response));
	}

	@Override
	public void setHistory(StorageReference object, Stream<TransactionReference> history) {
		recordTime(() -> {
			ByteIterable historyAsByteArray = intoByteArray(history.toArray(TransactionReference[]::new));
			ByteIterable objectAsByteArray = intoByteArray(object);
			storeOfHistory.put(txn, objectAsByteArray, historyAsByteArray);
		});
	}

	@Override
	public void setManifest(StorageReference manifest) {
		recordTime(() -> trieOfInfo.setManifest(manifest));
	}

	@Override
	public void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		// nothing to do, since Tendermint keeps the error message inside the blockchain, in the field "data" of its transactions
	}

	/**
	 * Starts a transaction. All updates during the transaction are saved
	 * in the supporting database if the transaction will later be committed.
	 */
	void beginTransaction() {
		txn = recordTime(env::beginTransaction);
		trieOfResponses = new TrieOfResponses(storeOfResponses, txn, nullIfEmpty(rootOfResponses));
		trieOfInfo = new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo));
	}

	/**
	 * Commits to the database all data put from the last call to {@linkplain #beginTransaction()}.
	 * 
	 * @return the hash of the state resulting at the end of all updates performed during the transaction
	 */
	byte[] commitTransaction() {
		return recordTime(() -> {
			// we increase the number of commits performed over this state
			trieOfInfo.setNumberOfCommits(trieOfInfo.getNumberOfCommits().add(BigInteger.ONE));
			if (!txn.commit())
				logger.info("Block transaction commit failed");
	
			byte[] result = new byte[64];
	
			byte[] rootOfResponses = trieOfResponses.getRoot();
			if (rootOfResponses != null)
				System.arraycopy(rootOfResponses, 0, result, 0, 32);
	
			byte[] rootOfInfo = trieOfInfo.getRoot();
			if (rootOfInfo != null)
				System.arraycopy(rootOfInfo, 0, result, 32, 32);
	
			return result;
		});
	}

	/**
	 * Resets the state to the given hash.
	 * 
	 * @param root the hash of the root to reset to
	 */
	void checkout(byte[] root) {
		splitRoots(root);
		recordTime(() -> env.executeInTransaction(txn -> storeOfRoot.put(txn, ROOT, ByteIterable.fromBytes(root))));
	}

	/**
	 * Yields the hash of this state.
	 * 
	 * @return the hash. If the state is currently empty, it yields an array of a single, 0 byte
	 */
	byte[] getHash() {
		if (isEmpty())
			return new byte[0]; // required by Tendermint as initial, empty application state

		byte[] result = new byte[64];
		System.arraycopy(rootOfResponses, 0, result, 0, 32);
		System.arraycopy(rootOfInfo, 0, result, 32, 32);

		return result;
	}

	/**
	 * Yields the number of commits already performed over this state.
	 * 
	 * @return the number of commits
	 */
	long getNumberOfCommits() {
		// this is only called outside a transaction, hence txn can never be used
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo)).getNumberOfCommits().longValue()));
	}

	/**
	 * Creates a state that gets persisted inside the given directory.
	 * It is initialized to the view of the root resulting from the given function.
	 * 
	 * @param node the node for which the state is being built
	 * @param dir the directory where the state is persisted
	 * @param rootSupplier the function that supplies the root
	 */
	private Store(TendermintBlockchainImpl node, String dir, BiFunction<io.hotmoka.xodus.env.Store, Transaction, byte[]> rootSupplier) {
		super(node);

		this.env = new Environment(dir);

		AtomicReference<io.hotmoka.xodus.env.Store> storeOfRoot = new AtomicReference<>();
		AtomicReference<io.hotmoka.xodus.env.Store> storeOfResponses = new AtomicReference<>();
		AtomicReference<io.hotmoka.xodus.env.Store> storeOfHistory = new AtomicReference<>();
		AtomicReference<io.hotmoka.xodus.env.Store> storeOfInfo = new AtomicReference<>();
	
		recordTime(() -> env.executeInTransaction(txn -> {
			storeOfRoot.set(env.openStoreWithoutDuplicates("root", txn));
			storeOfResponses.set(env.openStoreWithoutDuplicates("responses", txn));
			storeOfHistory.set(env.openStoreWithoutDuplicates("history", txn));
			storeOfInfo.set(env.openStoreWithoutDuplicates("info", txn));
			splitRoots(rootSupplier.apply(storeOfRoot.get(), txn));
		}));

		this.storeOfRoot = storeOfRoot.get();
		this.storeOfResponses = storeOfResponses.get();
		this.storeOfHistory = storeOfHistory.get();
		this.storeOfInfo = storeOfInfo.get();
	}

	private boolean isEmpty() {
		for (byte b: rootOfResponses)
			if (b != (byte) 0)
				return false;
	
		for (byte b: rootOfInfo)
			if (b != (byte) 0)
				return false;
	
		return true;
	}

	private static byte[] nullIfEmpty(byte[] hash) {
		for (byte b: hash)
			if (b != (byte) 0)
				return hash;
	
		return null;
	}

	private void splitRoots(byte[] root) {
		System.arraycopy(root, 0, rootOfResponses, 0, 32);
		System.arraycopy(root, 32, rootOfInfo, 0, 32);
	}

	private static ByteIterable intoByteArray(StorageReference reference) throws UncheckedIOException {
		try {
			return ByteIterable.fromBytes(reference.toByteArrayWithoutSelector()); // more optimized than a normal marshallable
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static ByteIterable intoByteArray(Marshallable[] marshallables) throws UncheckedIOException {
		try {
			return ByteIterable.fromBytes(Marshallable.toByteArray(marshallables));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static <T extends Marshallable> T[] fromByteArray(Unmarshaller<T> unmarshaller, Function<Integer,T[]> supplier, ByteIterable bytes) throws UncheckedIOException {
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes.getBytes())))) {
			return Marshallable.unmarshallingOfArray(unmarshaller, supplier, ois);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}