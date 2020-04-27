package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;

/**
 * The state of the blockchain. It is a transactional database that keeps
 * information about the state of the objects created by the transactions executed
 * by the blockchain. Such information is not kept in blockchain, but only
 * its hash is stored in blockchain at the end of each block, for consensus.
 */
class State implements AutoCloseable {
	private final Environment env;

	/**
	 * The transaction that contains all changes from begin of block to commit of block.
	 */
	private Transaction txn;

	private Store responses;
	private final Map<TransactionReference, TransactionResponse> responsesRecent = new HashMap<>();

	private Store history;
    private final Map<StorageReference, TransactionReference[]> historyRecent = new HashMap<>();

    private Store info;

    /**
     * The name of the store where responses of transactions are kept.
     */
    private final static String RESPONSES = "responses";

    /**
     * The name of the store that keeps, for each storage reference, the list of
     * transaction references where that storage reference has been modified,
     * from youngest to oldest.
     */
    private final static String HISTORY = "history";

    /**
     * The name of the store that keeps information about the state, for
     * instance the height of the last processed block.
     */
    private final static String INFO = "info";

    /**
     * The key used inside the {@code INFO} store to keep the transaction reference
     * that installed the Takamaka base classes in blockchain.
     */
    private final static ByteIterable TAKAMAKA_CODE = ArrayByteIterable.fromByte((byte) 0);

    /**
     * The key used inside the {@code INFO} store to keep the transaction reference
     * that installed a user jar in blockchain, if any. This is mainly used to simplify the tests.
     */
    private final static ByteIterable JAR = ArrayByteIterable.fromByte((byte) 1);

    /**
     * The key used inside the {@code INFO} store to keep the storage references
     * of the initial accounts in blockchain, created in the constructor of
     * {@linkplain io.hotmoka.tendermint.internal.TendermintBlockchainImpl}.
     * This is an array of storage references, from the first account to the last account.
     */
    private final static ByteIterable ACCOUNTS = ArrayByteIterable.fromByte((byte) 2);

    /**
     * The key used inside the {@code INFO} store to keep the number of
     * commits executed with this state.
     */
    private final static ByteIterable COMMIT_COUNT = ArrayByteIterable.fromByte((byte) 3);

    /**
     * The key used inside the {@code INFO} store to know if the blockchain is initialized.
     */
    private final static ByteIterable INITIALIZED = ArrayByteIterable.fromByte((byte) 4);

    /**
     * The key used inside the {@code INFO} store to know the last committed transaction reference.
     */
    private final static ByteIterable NEXT = ArrayByteIterable.fromByte((byte) 4);

    private long stateTime;

    /**
     * Creates a state that gets persisted inside the given directory.
     * 
     * @param dir the directory where the state is persisted
     */
    State(String dir) {
    	long start = System.currentTimeMillis();
    	this.env = Environments.newInstance(dir);

    	// we enforce that all stores are created
    	env.executeInTransaction(txn -> {
    		responses = env.openStore(RESPONSES, StoreConfig.WITHOUT_DUPLICATES, txn);
            history = env.openStore(HISTORY, StoreConfig.WITHOUT_DUPLICATES, txn);
            info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
		});

    	stateTime += (System.currentTimeMillis() - start);
    }

    @Override
    public void close() {
    	//System.out.println("state time: " + stateTime);
    	if (txn != null && !txn.isFinished())
    		// blockchain closed with uncommitted transactions: we commit them
    		commitTransaction();

    	try {
    		env.close();
    	}
    	catch (ExodusException e) {
    		// this seems a big in Exodus: jetbrains.exodus.ExodusException: Finish all transactions before closing database environment
    	}
    }

    /**
     * Starts a transaction. All updates during the transaction are saved
     * if the transaction will later be committed. This is called at the beginning
     * of the execution of the transactions inside a block.
     */
	void beginTransaction() {
		long start = System.currentTimeMillis();
		txn = env.beginTransaction();
        responses = env.openStore(RESPONSES, StoreConfig.WITHOUT_DUPLICATES, txn);
        responsesRecent.clear();
        history = env.openStore(HISTORY, StoreConfig.WITHOUT_DUPLICATES, txn);
        historyRecent.clear();
        info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
        stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Commits all updates during the current transaction.
	 */
	void commitTransaction() {
		long start = System.currentTimeMillis();
		increaseNumberOfCommits();
		txn.commit();
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Puts in state the result of a transaction having the given reference.
	 * 
	 * @param transactionReference the reference of the transaction
	 * @param response the response of the transaction
	 * @throws IOException if the response cannot be saved in state
	 */
	void putResponseOf(TransactionReference transactionReference, TransactionResponse response) throws IOException {
		long start = System.currentTimeMillis();
		env.executeInTransaction(txn -> responses.put(txn, intoByteArray(transactionReference), intoByteArray(response)));
		responsesRecent.put(transactionReference, response);
		stateTime += (System.currentTimeMillis() - start);
	}

	void setHistory(StorageReference object, Stream<TransactionReference> history) {
		long start = System.currentTimeMillis();
		TransactionReference[] historyAsArray = history.toArray(TransactionReference[]::new);
		env.executeInTransaction(txn -> this.history.put(txn, intoByteArray(object), intoByteArray(historyAsArray)));
		historyRecent.put(object, historyAsArray);
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Puts in state the classpath of the transaction that installed the Takamaka
	 * base classes in blockchain.
	 * 
	 * @param takamakaCode the classpath
	 */
	void putTakamakaCode(Classpath takamakaCode) {
		long start = System.currentTimeMillis();
		env.executeInTransaction(txn -> info.put(txn, TAKAMAKA_CODE, intoByteArray(takamakaCode)));
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Puts in state the classpath of the transaction that installed a user jar in blockchain.
	 * This might be missing and is mainly used to simplify the tests.
	 * 
	 * @param takamakaCode the classpath
	 */
	void putJar(Classpath jar) {
		long start = System.currentTimeMillis();
		env.executeInTransaction(txn -> info.put(txn, JAR, intoByteArray(jar)));
		stateTime += (System.currentTimeMillis() - start);
	}

	void putNext(TransactionReference next) {
		long start = System.currentTimeMillis();
		env.executeInTransaction(txn -> info.put(txn, NEXT, intoByteArray(next)));
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Puts in state the storage reference to a new initial account.
	 * 
	 * @param account the storage reference of the account to add
	 */
	void addAccount(StorageReference account) {
		long start = System.currentTimeMillis();
		env.executeInTransaction(txn -> info.put(txn, ACCOUNTS, intoByteArray(Stream.concat(getAccounts(), Stream.of(account)).toArray(StorageReference[]::new))));
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Sets the initialized property in this state.
	 */
	void markAsInitialized() {
		long start = System.currentTimeMillis();
		env.executeInTransaction(txn -> info.put(txn, INITIALIZED, ByteIterable.EMPTY));
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Yields the result of a transaction having the given reference.
	 * 
	 * @param transactionReference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponseOf(TransactionReference transactionReference) {
		long start = System.currentTimeMillis();
		TransactionResponse result = responsesRecent.get(transactionReference);
		if (result != null)
			return Optional.of(result);

		return env.computeInReadonlyTransaction(txn -> {
			Store responses = env.openStore(RESPONSES, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable response = responses.get(txn, intoByteArray(transactionReference));
			stateTime += (System.currentTimeMillis() - start);
			return response == null ? Optional.empty() : Optional.of(fromByteArray(TransactionResponse::from, response));
		});
	}

	Optional<Stream<TransactionReference>> getHistoryOf(StorageReference object) {
		long start = System.currentTimeMillis();
		TransactionReference[] result = historyRecent.get(object);
		if (result != null)
			return Optional.of(Stream.of(result));

		return env.computeInReadonlyTransaction(txn -> {
			Store history = env.openStore(HISTORY, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable old = history.get(txn, intoByteArray(object));
			stateTime += (System.currentTimeMillis() - start);
			return old == null ? Optional.empty() : Optional.of(Stream.of(fromByteArray(TransactionReference::from, TransactionReference[]::new, old)));
		});
	}

	long getNumberOfCommits() {
		long start = System.currentTimeMillis();
		TransactionalComputable<Long> computable = txn -> {
			Store info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable count = info.get(txn, COMMIT_COUNT);
			stateTime += (System.currentTimeMillis() - start);
			return count == null ? 0L : Long.valueOf(new String(count.getBytesUnsafe()));
		};

		return env.computeInReadonlyTransaction(computable);
	}

	/**
	 * Yields the classpath of the Takamaka base classes in blockchain.
	 * 
	 * @return the classpath
	 */
	Optional<Classpath> getTakamakaCode() {
		long start = System.currentTimeMillis();
		return env.computeInReadonlyTransaction(txn -> {
			Store info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable takamakaCode = info.get(txn, TAKAMAKA_CODE);
			stateTime += (System.currentTimeMillis() - start);
			return takamakaCode == null ? Optional.empty() : Optional.of(fromByteArray(Classpath::from, takamakaCode));
		});
	}

	/**
	 * Yields the classpath of a user jar installed in blockchain, if any.
	 * This is mainly used to simplify the tests.
	 * 
	 * @return the classpath
	 */
	Optional<Classpath> getJar() {
		long start = System.currentTimeMillis();
		return env.computeInReadonlyTransaction(txn -> {
			Store info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable jar = info.get(txn, JAR);
			stateTime += (System.currentTimeMillis() - start);
			return jar == null ? Optional.empty() : Optional.of(fromByteArray(Classpath::from, jar));
		});
	}

	Optional<TransactionReference> getNext() {
		long start = System.currentTimeMillis();
		return env.computeInReadonlyTransaction(txn -> {
			Store info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable next = info.get(txn, NEXT);
			stateTime += (System.currentTimeMillis() - start);
			return next == null ? Optional.empty() : Optional.of(fromByteArray(LocalTransactionReference::from, next));
		});
	}

	/**
	 * Yields the initial accounts.
	 * 
	 * @return the accounts, as an ordered stream from the first to the last account
	 */
	Stream<StorageReference> getAccounts() {
		long start = System.currentTimeMillis();
		return env.computeInReadonlyTransaction(txn -> {
			Store info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable accounts = info.get(txn, ACCOUNTS);
			stateTime += (System.currentTimeMillis() - start);
			return accounts == null ? Stream.empty() : Stream.of((StorageReference[]) fromByteArray(StorageValue::from, StorageReference[]::new, accounts));
		});
	}

	/**
	 * Yields the initialized property from this state.
	 * 
	 * @return true if and only if {@code markAsInitialized()} has been already called
	 */
	boolean isInitialized() {
		long start = System.currentTimeMillis();

		TransactionalComputable<Boolean> computable = txn -> {
			Store info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
			stateTime += (System.currentTimeMillis() - start);
			return info.get(txn, INITIALIZED) != null;
		};

		return env.computeInReadonlyTransaction(computable);
	}

	private void increaseNumberOfCommits() {
		info.put(txn, COMMIT_COUNT, new ArrayByteIterable(Long.toString(getNumberOfCommits() + 1).getBytes()));
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
			return new ArrayByteIterable(reference.toByteArrayWithoutSelector());
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
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes.getBytesUnsafe()))) {
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
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes.getBytesUnsafe()))) {
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