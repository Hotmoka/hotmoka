package io.hotmoka.stores;

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
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.stores.internal.TrieOfInfo;
import io.hotmoka.stores.internal.TrieOfResponses;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Transaction;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.AbstractStore;

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions
 * but not their requests nor errors (for this reason it is <i>partial</i>).
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
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 */
public abstract class PartialTrieBasedStore<N extends AbstractNode<?,?>> extends AbstractStore<N> {

	/**
	 * The Xodus environment that holds the store.
	 */
	private final Environment env;

	/**
	 * The Xodus store that holds the root of the root.
	 */
    private final io.hotmoka.xodus.env.Store storeOfRoot;

    /**
	 * The Xodus store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final io.hotmoka.xodus.env.Store storeOfResponses;

	/**
	 * The Xodus store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final io.hotmoka.xodus.env.Store storeOfHistory;

	/**
	 * The Xodus store that holds miscellaneous information about the store.
	 */
    private final io.hotmoka.xodus.env.Store storeOfInfo;

	/**
	 * The root of the trie of the responses. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfResponses = new byte[32];

	/**
	 * The root of the trie of the miscellaneous info. It is an empty array if the trie is empty.
	 */
	private final byte[] rootOfInfo = new byte[32];

	/**
	 * The hashing algorithm used to merge the hashes of the many tries.
	 */
	private final HashingAlgorithm<byte[]> hashOfHashes;

	/**
	 * The key used inside {@linkplain storeOfRoot} to keep the root.
	 */
	private final static ByteIterable ROOT = ByteIterable.fromByte((byte) 0);

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
	 * The time when {@link #txn} was started, in the same format as {@link System#currentTimeMillis()}.
	 */
	private long now;

	/**
     * Creates the store initialized to the view of the last checked out root.
     * 
     * @param node the node for which the store is being built
     */
	protected PartialTrieBasedStore(N node) {
    	this(node, (storeOfRoot, txn) -> {
    		ByteIterable root = storeOfRoot.get(txn, ROOT);
    		return root == null ? new byte[64] : root.getBytes();
    	});
    }

    /**
     * Creates a store initialized to the view of the given root.
     * 
	 * @param node the node for which the store is being built
     * @param hash the root to use for the store
     */
    protected PartialTrieBasedStore(N node, byte[] hash) {
    	this(node, (_storeOfRoot, _txn) -> hash);
    }

    @Override
    public void close() {
    	if (txn != null && !txn.isFinished()) {
    		// blockchain closed with yet uncommitted transactions: we abort them
    		logger.error("Store closed with uncommitted transactions: they are being aborted");
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
	public long getNow() {
		return now;
	}

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> new TrieOfResponses(storeOfResponses, txn, nullIfEmpty(rootOfResponses)).get(reference)));
	}

	@Override
	public Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		// this method uses the last updates to the store, possibly also consequence of
		// transactions inside the current block; however, it might be called also when
		// a block has been committed and the next is not yet started; this can occur for
		// runView transaction methods.
		// Moreover, txn might be null if the first transaction that gets executed
		// with a store is a runView transaction and no new node has been created yet.
		// This might happen for instance upon node recreation
		if (txn == null || txn.isFinished())
			return getResponse(reference);
		else
			return recordTime(() -> trieOfResponses.get(reference));
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
	protected void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		recordTime(() -> trieOfResponses.put(reference, response));
	}

	@Override
	protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
		recordTime(() -> {
			ByteIterable historyAsByteArray = intoByteArray(history.toArray(TransactionReference[]::new));
			ByteIterable objectAsByteArray = intoByteArray(object);
			storeOfHistory.put(txn, objectAsByteArray, historyAsByteArray);
		});
	}

	@Override
	protected void setManifest(StorageReference manifest) {
		recordTime(() -> trieOfInfo.setManifest(manifest));
	}

	/**
	 * Starts a transaction. All updates during the transaction are saved
	 * in the supporting database if the transaction will later be committed.
	 * 
	 * @param now the time to use as starting moment of the transaction
	 */
	public void beginTransaction(long now) {
		txn = recordTime(env::beginTransaction);
		trieOfResponses = new TrieOfResponses(storeOfResponses, txn, nullIfEmpty(rootOfResponses));
		trieOfInfo = new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo));
		this.now = now;
	}

	/**
	 * Commits to the database all data put from the last call to {@linkplain #beginTransaction()}.
	 * This data does not change the view of the store, unless the resulting hash gets later checked out.
	 * 
	 * @return the hash of the store resulting at the end of all updates performed during the transaction
	 */
	public byte[] commitTransaction() {
		return recordTime(() -> {
			// we increase the number of commits performed over this store
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
	 * Resets the store to the given hash.
	 * 
	 * @param root the hash of the root to reset to
	 */
	public void checkout(byte[] root) {
		splitRoots(root);
		recordTime(() -> env.executeInTransaction(txn -> storeOfRoot.put(txn, ROOT, ByteIterable.fromBytes(root))));
	}

	/**
	 * Yields the hash of this store.
	 * 
	 * @return the hash. If the store is currently empty, it yields an array of a single, 0 byte
	 */
	public byte[] getHash() {
		if (isEmpty())
			return new byte[0];

		byte[] result = new byte[64];
		System.arraycopy(rootOfResponses, 0, result, 0, 32);
		System.arraycopy(rootOfInfo, 0, result, 32, 32);

		// we hash the result into 32 bytes
		return hashOfHashes.hash(result);
	}

	/**
	 * Yields the number of commits already performed over this store.
	 * 
	 * @return the number of commits
	 */
	public long getNumberOfCommits() {
		// this is meaningful only wrt the number of committed transactions, hence this.txn is not used
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> new TrieOfInfo(storeOfInfo, txn, nullIfEmpty(rootOfInfo)).getNumberOfCommits().longValue()));
	}

	/**
	 * Creates a store initialized to the view of the root resulting from the given function.
	 * 
	 * @param node the node for which the store is being built
	 * @param rootSupplier the function that supplies the root
	 */
	private PartialTrieBasedStore(N node, BiFunction<io.hotmoka.xodus.env.Store, Transaction, byte[]> rootSupplier) {
		super(node);

		try {
			this.hashOfHashes = HashingAlgorithm.sha256((byte[] bytes) -> bytes);
			this.env = new Environment(node.config.dir + "/store");

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
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
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
			throw InternalFailureException.of(e);
		}
	}
}