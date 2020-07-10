package io.hotmoka.tendermint.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.patricia.PatriciaTrie;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;
import io.hotmoka.xodus.env.Environment;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;
import io.takamaka.code.engine.AbstractNodeWithHistory;
import io.takamaka.code.engine.StateUpdate;

/**
 * A historical state of a node. It is a transactional database that keeps
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
class State implements AutoCloseable {

	/**
	 * The Xodus environment that holds the state.
	 */
	private final Environment env;

	/**
	 * The store that holds the root of the state.
	 */
    private final Store storeOfRoot;

    /**
	 * The store that holds the Merkle-Patricia trie of the responses to the requests.
	 */
	private final Store storeOfResponses;

	/**
	 * The store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private final Store storeOfHistory;

	/**
	 * The store that holds miscellaneous information about the state.
	 */
    private final Store storeOfInfo;

    /**
	 * The hashing algorithm applied to transaction references when used as
	 * keys of a Merkle-Patricia trie. Since these keys are transaction references,
	 * they already hold a hash, as a string. Hence, this algorithm just amounts to extracting
	 * the bytes from that string.
	 */
	private final HashingAlgorithm<TransactionReference> hashingForTransactionReferences = new HashingAlgorithm<>() {
	
		@Override
		public byte[] hash(TransactionReference reference) {
			return hexStringToByteArray(reference.getHash());
		}
	
		@Override
		public int length() {
			return 32; // transaction references are assumed to be SHA256 hashes, hence 32 bytes
		}
	
		/**
		 * Transforms a hexadecimal string into a byte array.
		 * 
		 * @param s the string
		 * @return the byte array
		 */
		private byte[] hexStringToByteArray(String s) {
		    int len = s.length();
		    byte[] data = new byte[len / 2];
		    for (int i = 0; i < len; i += 2)
		        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		
		    return data;
		}
	};

	/**
	 * The hashing algorithm applied to the nodes of the Merkle-Patricia tries.
	 */
	private final HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes;

	/**
     * The key used inside {@linkplain storeOfRoot} to keep the root.
     */
    private final static ByteIterable ROOT = ByteIterable.fromByte((byte) 0);

    /**
     * The key used inside {@linkplain #storeOfInfo} to keep the number of commits executed over this state.
     */
    private final static ByteIterable COMMIT_COUNT = ByteIterable.fromByte((byte) 0);

    /**
     * The key used inside {@linkplain #storeOfInfo} to keep the storage reference of the manifest of the node.
     */
    private final static ByteIterable MANIFEST = ByteIterable.fromByte((byte) 1);

    private final static Logger logger = LoggerFactory.getLogger(State.class);

    /**
	 * The root of the trie of the responses.
	 */
	private byte[] rootOfResponses;

	/**
	 * The time spent inside the state procedures, for profiling.
	 */
	private long stateTime;

	/**
	 * The transaction that accumulates all changes from begin of block to commit of block.
	 */
	private Transaction txn;

	/**
     * The trie of the responses.
     */
	private PatriciaTrie<TransactionReference, TransactionResponse> trieOfResponses;

	/**
     * Creates a state that gets persisted inside the given directory.
     * It is initialized to the view of the last checked out root.
     * 
     * @param dir the directory where the state is persisted
     * @throws NoSuchAlgorithmException if the algorithm for hashing the nodes of the Patricia trie is not available
     */
    State(String dir) throws NoSuchAlgorithmException {
    	this(dir, (storeOfRoot, txn) -> {
    		ByteIterable root = storeOfRoot.get(txn, ROOT);
    		return root == null ? null : root.getBytes();
    	});
    }

    /**
     * Creates a state that gets persisted inside the given directory.
     * It is initialized to the view of the given root.
     * 
     * @param dir the directory where the state is persisted
     * @param hash the root to use for the state
     * @throws NoSuchAlgorithmException if the algorithm for hashing the nodes of the Patricia trie is not available
     */
    State(String dir, byte[] hash) throws NoSuchAlgorithmException {
    	this(dir, (_storeOfRoot, _txn) -> hash);
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

    	logger.info("Time spent in state procedures: " + stateTime + "ms");
    }

    /**
     * Starts a transaction. All updates during the transaction are saved
     * in the supporting database if the transaction will later be committed.
     */
    void beginTransaction() {
    	txn = recordTime(env::beginTransaction);
    	KeyValueStoreOnXodus keyValueStoreOfResponses = new KeyValueStoreOnXodus(storeOfResponses, txn, rootOfResponses);
    	trieOfResponses = PatriciaTrie.of(keyValueStoreOfResponses, hashingForTransactionReferences, hashingForNodes, TransactionResponse::from);
    }

    /**
	 * Pushes into state the result of executing a successful Hotmoka request. This method
	 * is called, possibly many times, during a transaction, between {@link #beginTransaction()} and
	 * {@link #commitTransaction()}, hence {@linkplain #txn} exists and is not yet committed.
	 * 
	 * 
	 * @param node the node having this state
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	void push(AbstractNodeWithHistory<?> node, TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		new StateUpdate(node, reference, request, response) {
	
			@Override
			protected void pushInStore(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
				recordTime(() -> trieOfResponses.put(reference, response));
			}
	
			@Override
			protected Stream<TransactionReference> getHistory(StorageReference object) {
				return recordTime(() -> {
					ByteIterable historyAsByteArray = storeOfHistory.get(txn, intoByteArray(object));
					return historyAsByteArray == null ? Stream.empty() : Stream.of(fromByteArray(TransactionReference::from, TransactionReference[]::new, historyAsByteArray));
				});
			}
	
			@Override
			protected void setHistory(StorageReference object, Stream<TransactionReference> history) {
				recordTime(() -> {
					ByteIterable historyAsByteArray = intoByteArray(history.toArray(TransactionReference[]::new));
					ByteIterable objectAsByteArray = intoByteArray(object);
					State.this.storeOfHistory.put(txn, objectAsByteArray, historyAsByteArray);
				});
			}
	
			@Override
			protected void initialize(StorageReference manifest) {
				recordTime(() -> storeOfInfo.put(txn, MANIFEST, intoByteArray(manifest)));
			}
		};
	}

	/**
	 * Commits to the database all data put from the last call to {@linkplain #beginTransaction()}.
	 * 
	 * @return the hash of the state resulting at the end of all updates performed during the transaction
	 */
    byte[] commitTransaction() {
    	return recordTime(() -> {
    		// we increase the number of commits performed over this state
    		ByteIterable numberOfCommitsAsByteIterable = storeOfInfo.get(txn, COMMIT_COUNT);
    		long numberOfCommits = numberOfCommitsAsByteIterable == null ? 0L : Long.valueOf(new String(numberOfCommitsAsByteIterable.getBytes()));
    		storeOfInfo.put(txn, COMMIT_COUNT, ByteIterable.fromBytes(Long.toString(numberOfCommits + 1).getBytes()));
    		if (!txn.commit())
    			logger.info("Block transaction commit failed");

    		return trieOfResponses.getRoot();
    	});
    }

    /**
     * Resets the state to the given hash.
     * 
     * @param hash the hash
     */
	void checkout(byte[] hash) {
		this.rootOfResponses = hash;
		recordTime(() -> env.executeInTransaction(txn -> storeOfRoot.put(txn, ROOT, ByteIterable.fromBytes(hash))));
	}

	/**
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponse(TransactionReference reference) {
		return recordTime(() -> { 
			return env.computeInReadonlyTransaction(txn -> {
				PatriciaTrie<TransactionReference, TransactionResponse> trie
					= PatriciaTrie.of(new KeyValueStoreOnXodus(storeOfResponses, txn, rootOfResponses), hashingForTransactionReferences, hashingForNodes, TransactionResponse::from);
				return trie.get(reference);
			});
		});
	}

	/**
	 * Yields the response of the transaction having the given reference.
	 * The response if returned also when it is in the current transaction, not yet committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
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

	/**
	 * Yields the history of the given object, that is, the references of the transactions
	 * that provide information about the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history. Yields an empty stream if there is no history for {@code object}
	 */
	Stream<TransactionReference> getHistory(StorageReference object) {
		return recordTime(() -> {
			ByteIterable historyAsByteArray = env.computeInReadonlyTransaction(txn -> storeOfHistory.get(txn, intoByteArray(object)));
			return historyAsByteArray == null ? Stream.empty() : Stream.of(fromByteArray(TransactionReference::from, TransactionReference[]::new, historyAsByteArray));
		});
	}

	/**
	 * Yields the number of commits already performed over this state.
	 * 
	 * @return the number of commits
	 */
	long getNumberOfCommits() {
		ByteIterable numberOfCommitsAsByteIterable = getFromInfo(COMMIT_COUNT);
		return numberOfCommitsAsByteIterable == null ? 0L : Long.valueOf(new String(numberOfCommitsAsByteIterable.getBytes()));
	}

	/**
	 * Yields the manifest installed when the node is initialized.
	 * 
	 * @return the manifest
	 */
	Optional<StorageReference> getManifest() {
		ByteIterable manifest = getFromInfo(MANIFEST);
		return manifest == null ? Optional.empty() : Optional.of(fromByteArray(StorageReference::from, manifest));
	}

	/**
	 * Yields the hash of this state.
	 * 
	 * @return the hash. If the state is currently empty, it yields an array of a single, 0 byte
	 */
	byte[] getHash() {
		return rootOfResponses == null ? new byte[0] : rootOfResponses;
	}

	/**
	 * Creates a state that gets persisted inside the given directory.
	 * It is initialized to the view of the root resulting from the given function.
	 * 
	 * @param dir the directory where the state is persisted
	 * @param rootSupplier the function thatr supplies the root
	 * @throws NoSuchAlgorithmException if the algorithm for hashing the nodes of the Patricia trie is not available
	 */
	private State(String dir, BiFunction<Store, Transaction, byte[]> rootSupplier) throws NoSuchAlgorithmException {
		this.env = new Environment(dir);
		this.hashingForNodes = HashingAlgorithm.sha256(Marshallable::toByteArray);

		AtomicReference<Store> storeOfRoot = new AtomicReference<>();
		AtomicReference<Store> storeOfResponses = new AtomicReference<>();
		AtomicReference<Store> storeOfHistory = new AtomicReference<>();
		AtomicReference<Store> storeOfInfo = new AtomicReference<>();
	
		recordTime(() -> env.executeInTransaction(txn -> {
			storeOfRoot.set(env.openStoreWithoutDuplicates("root", txn));
			storeOfResponses.set(env.openStoreWithoutDuplicates("responses", txn));
			storeOfHistory.set(env.openStoreWithoutDuplicates("history", txn));
			storeOfInfo.set(env.openStoreWithoutDuplicates("info", txn));
	    	rootOfResponses = rootSupplier.apply(storeOfRoot.get(), txn);
		}));

		this.storeOfRoot = storeOfRoot.get();
		this.storeOfResponses = storeOfResponses.get();
		this.storeOfHistory = storeOfHistory.get();
		this.storeOfInfo = storeOfInfo.get();
	}

	/**
	 * Yields the value of the given property in the {@linkplain #storeOfInfo} store.
	 * 
	 * @return true if and only if {@code markAsInitialized()} has been already called
	 */
	private ByteIterable getFromInfo(ByteIterable key) {
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> storeOfInfo.get(txn, key)));
	}

	/**
	 * Executes the given task, taking note of the time required for it.
	 * 
	 * @param task the task
	 */
	private void recordTime(Runnable task) {
		long start = System.currentTimeMillis();
		task.run();
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Executes the given task, taking note of the time required for it.
	 * 
	 * @param task the task
	 */
	private <T> T recordTime(Supplier<T> task) {
		long start = System.currentTimeMillis();
		T result = task.get();
		stateTime += (System.currentTimeMillis() - start);
		return result;
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

	private static <T extends Marshallable> T fromByteArray(Unmarshaller<T> unmarshaller, ByteIterable bytes) throws UncheckedIOException {
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes.getBytes())))) {
			return unmarshaller.from(ois);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
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