package io.hotmoka.tendermint.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.patricia.KeyValueStore;
import io.hotmoka.patricia.PatriciaTrie;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;

/**
 * The state of a blockchain built over Tendermint. It is a transactional database that keeps
 * information about the state of the objects created by the transactions executed
 * by the blockchain. This state is external to the blockchain and only
 * its hash is stored in blockchain at the end of each block, for consensus.
 * The information kept in this state consists of:
 * 
 * <ul>
 * <li> a map from each Hotmoka transaction reference to the response computed for that transaction
 *      (implemented as a Patricia trie supported by a store)
 * <li> a map from each storage reference to the transaction references that contribute
 *      to provide values to the fields of the storage object at that reference
 * <li> some miscellaneous control information, such as  where the jar with basic
 *      Takamaka classes is installed or the current number of commits
 * </ul>
 * 
 * This information is added in store by put methods and accessed through get methods.
 * Information between paired begin transaction and commit transaction is committed into
 * the file system.
 * 
 * The implementation of this state uses JetBrains's Xodus transactional database.
 */
class State implements AutoCloseable {

	/**
	 * The Xodus environment that holds the state.
	 */
	private final Environment env;

	/**
	 * The transaction that accumulates all changes from begin of block to commit of block.
	 */
	private Transaction txn;

	/**
	 * The store that holds the Merkle-Patricia trie of the responses to the transactions.
	 */
	private Store patricia;

	/**
	 * The store that holds the history of each storage reference, ie, a list of
	 * transaction references that contribute
	 * to provide values to the fields of the storage object at that reference.
	 */
	private Store history;

	/**
	 * The store that holds miscellaneous information about the state.
	 */
    private Store info;

    /**
	 * The time spent inside the state procedures, for profiling.
	 */
	private long stateTime;

	/**
     * The key used inside {@linkplain #info} to keep the transaction reference
     * that installed the Takamaka base classes in blockchain.
     */
    private final static ByteIterable TAKAMAKA_CODE = ArrayByteIterable.fromByte((byte) 0);

    /**
     * The key used inside {@linkplain #info} to keep the number of commits executed over this state.
     */
    private final static ByteIterable COMMIT_COUNT = ArrayByteIterable.fromByte((byte) 1);

    /**
     * The key used inside {@linkplain #info} to keep note that the node is initialized.
     */
    private final static ByteIterable INITIALIZED = ArrayByteIterable.fromByte((byte) 2);

    /**
     * The key used inside {@linkplain #info} to keep the hash of the root of the Patricia trie
     * of the responses.
     */
    private final static ByteIterable ROOT = ArrayByteIterable.fromByte((byte) 3);

    private final static Logger logger = LoggerFactory.getLogger(State.class);

    /**
     * The hashing algorithm applied to the keys of the Merkle-Patricia trie.
     * Since these keys are transaction reference, they are already hashes. Hence,
     * this algorithm just amounts to extracting the bytes from the reference.
     */
    private final HashingAlgorithm<TransactionReference> hashingForTransactionReferences = new HashingAlgorithm<>() {

		@Override
		public byte[] hash(TransactionReference reference) {
			return hexStringToByteArray(reference.getHash());
		}

		@Override
		public int length() {
			return 32; // transaction references in this blockchain are SHA256 hashes, hence 32 bytes
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
     * The hashing algorithm applied to the nodes of the Merkle-Patricia trie.
     */
    private final HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes;
    
    /**
     * Creates a state that gets persisted inside the given directory.
     * 
     * @param dir the directory where the state is persisted
     * @throws NoSuchAlgorithmException if the algorithm for hashing the nodes of the Patricia trie
     *                                  is not available
     */
    State(String dir) throws NoSuchAlgorithmException {
    	this.env = Environments.newInstance(dir);
    	this.hashingForNodes = HashingAlgorithm.sha256();

    	// enforces that all stores exist
    	recordTime(() ->
    		env.executeInTransaction(txn -> {
    			patricia = env.openStore("patricia", StoreConfig.WITHOUT_DUPLICATES, txn);
    			history = env.openStore("history", StoreConfig.WITHOUT_DUPLICATES, txn);
    			info = env.openStore("info", StoreConfig.WITHOUT_DUPLICATES, txn);
    		})
    	);
    }

    @Override
    public void close() {
    	if (txn != null && !txn.isFinished())
    		// blockchain closed with yet uncommitted transactions: we commit them
    		if (!txn.commit())
    			logger.error("Transaction commit returned false");

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
     * if the transaction will later be committed. This is called at the beginning
     * of the execution of the transactions inside a block.
     */
    void beginTransaction() {
    	recordTime(() -> {
    		txn = env.beginTransaction();
    		patricia = env.openStore("patricia", StoreConfig.USE_EXISTING, txn);
    		history = env.openStore("history", StoreConfig.USE_EXISTING, txn);
    		info = env.openStore("info", StoreConfig.USE_EXISTING, txn);
    	});
    }

	/**
	 * Commits all data put from last call to {@linkplain #beginTransaction()}.
	 */
	void commitTransaction() {
		recordTime(() -> {
			increaseNumberOfCommits();
			if (!txn.commit())
				logger.info("Transaction commit returned false");
		});
	}

	/**
	 * Puts in state the result of a transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param response the response of the transaction
	 */
	void putResponse(TransactionReference reference, TransactionResponse response) {
		recordTime(() -> env.executeInTransaction(txn -> getTrie(txn).put(reference, response)));
	}

	/**
	 * Yields a key/value store that uses the given transaction for reading or writing data.
	 * 
	 * @param txn the transaction
	 * @return the key/value store
	 */
	private KeyValueStore getKeyValueStore(Transaction txn) {
		return new KeyValueStore() {

			@Override
			public byte[] getRoot() {
				ByteIterable root = info.get(txn, ROOT);
				return root == null ? null : root.getBytesUnsafe();
			}

			@Override
			public void setRoot(byte[] root) {
				info.put(txn, ROOT, new ArrayByteIterable(root));
			}

			@Override
			public void put(byte[] key, byte[] value) {
				patricia.put(txn, new ArrayByteIterable(key), new ArrayByteIterable(value));
			}

			@Override
			public byte[] get(byte[] key) throws NoSuchElementException {
				ByteIterable result = patricia.get(txn, new ArrayByteIterable(key));
				if (result == null)
					throw new NoSuchElementException("no Merkle-Patricia trie node");
				else
					return result.getBytesUnsafe();
			}
		};
	}

	/**
	 * Sets the history of the given object.
	 * 
	 * @param object the object
	 * @param history the history, that is, the transaction references transaction references that contribute
	 *                to provide values to the fields of {@code object}
	 */
	void putHistory(StorageReference object, Stream<TransactionReference> history) {
		recordTime(() -> {
			ByteIterable historyAsByteArray = intoByteArray(history.toArray(TransactionReference[]::new));
			ByteIterable objectAsByteArray = intoByteArray(object);
			env.executeInTransaction(txn -> this.history.put(txn, objectAsByteArray, historyAsByteArray));
		});
	}

	/**
	 * Puts in state the classpath of the transaction that installed the Takamaka
	 * base classes in blockchain.
	 * 
	 * @param takamakaCode the classpath
	 */
	void putTakamakaCode(Classpath takamakaCode) {
		putIntoInfo(TAKAMAKA_CODE, takamakaCode);
	}

	/**
	 * Takes note that the node is initialized.
	 */
	void initialize() {
		recordTime(() -> env.executeInTransaction(txn -> info.put(txn, INITIALIZED, INITIALIZED)));
	}

	/**
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponse(TransactionReference reference) {
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> {
			try {
				return Optional.of(getTrie(txn).get(reference));
			}
			catch (NoSuchElementException e) {
				return Optional.empty();
			}
		}));
	}

	/**
	 * Yields the Merkle-Patricia trie for this state.
	 * 
	 * @param txn the transaction for which the trie is being built
	 * @return the trie
	 */
	private PatriciaTrie<TransactionReference, TransactionResponse> getTrie(Transaction txn) {
		KeyValueStore keyValueStore = getKeyValueStore(txn);
		return PatriciaTrie.of(keyValueStore, hashingForTransactionReferences, hashingForNodes, TransactionResponse::from);
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
			ByteIterable historyAsByteArray = env.computeInReadonlyTransaction(txn -> history.get(txn, intoByteArray(object)));
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
		return numberOfCommitsAsByteIterable == null ? 0L : Long.valueOf(new String(numberOfCommitsAsByteIterable.getBytesUnsafe()));
	}

	/**
	 * Yields the classpath of the Takamaka base classes in blockchain.
	 * 
	 * @return the classpath
	 */
	Optional<Classpath> getTakamakaCode() {
		ByteIterable takamakaCode = getFromInfo(TAKAMAKA_CODE);
		return takamakaCode == null ? Optional.empty() : Optional.of(fromByteArray(Classpath::from, takamakaCode));
	}

	boolean isInitialized() {
		return getFromInfo(INITIALIZED) != null;
	}

	/**
	 * Yields the hash of this state.
	 * 
	 * @return the hash. If the state is currently empty, it yields an array of a single, 0 byte
	 */
	byte[] getHash() {
		ByteIterable hashOfTrieRoot = getFromInfo(ROOT);
		if (hashOfTrieRoot == null)
			return new byte[0];
		else
			return hashOfTrieRoot.getBytesUnsafe();
	}

	/**
	 * Increases the number of commits performed over this state.
	 */
	private void increaseNumberOfCommits() {
		recordTime(() -> 
			env.executeInTransaction(txn -> {
				ByteIterable numberOfCommitsAsByteIterable = info.get(txn, COMMIT_COUNT);
				long numberOfCommits = numberOfCommitsAsByteIterable == null ? 0L : Long.valueOf(new String(numberOfCommitsAsByteIterable.getBytesUnsafe()));
				info.put(txn, COMMIT_COUNT, new ArrayByteIterable(Long.toString(numberOfCommits + 1).getBytes()));
			}));
	}

	/**
	 * Yields the value of the given property in the {@linkplain #info} store.
	 * 
	 * @return true if and only if {@code markAsInitialized()} has been already called
	 */
	private ByteIterable getFromInfo(ByteIterable key) {
		return recordTime(() -> env.computeInReadonlyTransaction(txn -> info.get(txn, key)));
	}

	/**
	 * Puts in state the given value, inside the {@linkplain #info} store.
	 * 
	 * @param key the key where the value must be put
	 * @param value the value to put
	 */
	private void putIntoInfo(ByteIterable key, Marshallable value) {
		recordTime(() -> {
			ByteIterable valueAAsByteArray = intoByteArray(value);
			env.executeInTransaction(txn -> info.put(txn, key, valueAAsByteArray));
		});
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

	private interface TimedTask<T> {
		T call();
	}

	/**
	 * Executes the given task, taking note of the time required for it.
	 * 
	 * @param task the task
	 */
	private <T> T recordTime(TimedTask<T> task) {
		long start = System.currentTimeMillis();
		T result = task.call();
		stateTime += (System.currentTimeMillis() - start);
		return result;
	}

	private static ArrayByteIterable intoByteArray(Marshallable marshallable) throws UncheckedIOException {
		try {
			return new ArrayByteIterable(marshallable.toByteArray());
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static ArrayByteIterable intoByteArray(StorageReference reference) throws UncheckedIOException {
		try {
			return new ArrayByteIterable(reference.toByteArrayWithoutSelector()); // more optimized than a normal marshallable
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static ArrayByteIterable intoByteArray(Marshallable[] marshallables) throws UncheckedIOException {
		try {
			return new ArrayByteIterable(Marshallable.toByteArray(marshallables));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static <T extends Marshallable> T fromByteArray(Unmarshaller<T> unmarshaller, ByteIterable bytes) throws UncheckedIOException {
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes.getBytesUnsafe())))) {
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
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes.getBytesUnsafe())))) {
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