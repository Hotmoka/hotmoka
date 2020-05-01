package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.engine.LRUCache;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;

/**
 * The state of the blockchain. It is a transactional database that keeps
 * information about the state of the objects created by the transactions executed
 * by the blockchain. Such information is not kept in blockchain, but only
 * its hash is stored in blockchain at the end of each block, for consensus.
 */
class State implements AutoCloseable {

	/**
	 * The exodus environment that holds the state.
	 */
	private final Environment env;

	/**
	 * The transaction that accumulates all changes from begin of block to commit of block.
	 */
	private Transaction txn;

	/**
	 * The store that holds the responses to the transactions.
	 */
	private Store responses;

	/**
	 * The store that holds the history of each storage reference, ie, a list of
	 * transaction references where the storage reference has been updated.
	 */
	private Store history;

	/**
	 * A cache in memory to speed up access to {@linkplain #history}.
	 */
	private final LRUCache<StorageReference, TransactionReference[]> historyRecent = new LRUCache<>(10_000);

	/**
	 * The store the holds miscellaneous information about the state.
	 */
    private Store info;

    /**
     * The key used inside {@linkplain #info} to keep the transaction reference
     * that installed the Takamaka base classes in blockchain.
     */
    private final static ByteIterable TAKAMAKA_CODE = ArrayByteIterable.fromByte((byte) 0);

    /**
     * The key used inside {@linkplain #info} to keep the transaction reference
     * that installed a user jar in blockchain, if any. This is mainly used to simplify the tests.
     */
    private final static ByteIterable JAR = ArrayByteIterable.fromByte((byte) 1);

    /**
     * The key used inside {@linkplain #info} to keep the storage references
     * of the initial accounts in blockchain, created in the constructor of
     * {@linkplain io.hotmoka.tendermint.internal.TendermintBlockchainImpl}.
     * This is an array of storage references, from the first account to the last account.
     */
    private final static ByteIterable ACCOUNTS = ArrayByteIterable.fromByte((byte) 2);

    /**
     * The key used inside {@linkplain #info} to keep the number of commits executed over this state.
     */
    private final static ByteIterable COMMIT_COUNT = ArrayByteIterable.fromByte((byte) 3);

    /**
     * The key used inside {@linkplain #info} to know if the blockchain is initialized.
     */
    private final static ByteIterable INITIALIZED = ArrayByteIterable.fromByte((byte) 4);

    /**
     * The key used inside {@linkplain #info} to know the last committed transaction reference.
     */
    private final static ByteIterable NEXT = ArrayByteIterable.fromByte((byte) 4);

    /**
     * The time spent inside the state procedures, for profiling.
     */
    private long stateTime;

    private final static Logger logger = LoggerFactory.getLogger(State.class);

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
    		responses = env.openStore("responses", StoreConfig.WITHOUT_DUPLICATES, txn);
            history = env.openStore("history", StoreConfig.WITHOUT_DUPLICATES, txn);
            info = env.openStore("info", StoreConfig.WITHOUT_DUPLICATES, txn);
		});

    	stateTime += (System.currentTimeMillis() - start);
    }

    @Override
    public void close() {
    	if (txn != null && !txn.isFinished())
    		// blockchain closed with yet uncommitted transactions: we commit them
    		if (!txn.commit())
    			logger.info("transaction commit returned false");

    	try {
    		env.close();
    	}
    	catch (ExodusException e) {
    		logger.info("failed to close environment", e);
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
        responses = env.openStore("responses", StoreConfig.WITHOUT_DUPLICATES, txn);
        history = env.openStore("history", StoreConfig.WITHOUT_DUPLICATES, txn);
        info = env.openStore("info", StoreConfig.WITHOUT_DUPLICATES, txn);
        stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Commits all updates occurred during the current transaction.
	 */
	void commitTransaction() {
		long start = System.currentTimeMillis();
		increaseNumberOfCommits();
		if (!txn.commit())
			logger.info("transaction commit returned false");

		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Puts in state the result of a transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param response the response of the transaction
	 * @throws IOException if the response cannot be saved in state
	 */
	void putResponse(TransactionReference reference, TransactionResponse response) throws IOException {
		long start = System.currentTimeMillis();
		env.executeInTransaction(txn -> responses.put(txn, intoByteArray(reference), intoByteArray(response)));
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Sets the history of the given object.
	 * 
	 * @param object the object
	 * @param history the history, that is, the references to the transactions that
	 *                can contain the current values of the fields of the object
	 */
	void setHistory(StorageReference object, Stream<TransactionReference> history) {
		long start = System.currentTimeMillis();
		TransactionReference[] historyAsArray = history.toArray(TransactionReference[]::new);
		ArrayByteIterable historyAsByteArray = intoByteArray(historyAsArray);
		ArrayByteIterable objectAsByteArray = intoByteArray(object);
		env.executeInTransaction(txn -> this.history.put(txn, objectAsByteArray, historyAsByteArray));
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
		ArrayByteIterable takamakaCodeAsByteArray = intoByteArray(takamakaCode);
		env.executeInTransaction(txn -> info.put(txn, TAKAMAKA_CODE, takamakaCodeAsByteArray));
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
		ArrayByteIterable jarAsByteArray = intoByteArray(jar);
		env.executeInTransaction(txn -> info.put(txn, JAR, jarAsByteArray));
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Puts in state the reference that can be used for the next transaction.
	 * 
	 * @param next the reference
	 */
	void putNext(TransactionReference next) {
		long start = System.currentTimeMillis();
		ArrayByteIterable nextAsByteArray = intoByteArray(next);
		env.executeInTransaction(txn -> info.put(txn, NEXT, nextAsByteArray));
		stateTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Puts in state the storage reference to a new initial account.
	 * 
	 * @param account the storage reference of the account to add
	 */
	void addAccount(StorageReference account) {
		long start = System.currentTimeMillis();
		ArrayByteIterable accountsAsByteArray = intoByteArrayWithoutSelector(Stream.concat(getAccounts(), Stream.of(account)).toArray(StorageReference[]::new));
		env.executeInTransaction(txn -> info.put(txn, ACCOUNTS, accountsAsByteArray));
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
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponse(TransactionReference reference) {
		long start = System.currentTimeMillis();

		ArrayByteIterable referenceAsByteArray = intoByteArray(reference);
		ByteIterable responseAsByteArray = env.computeInReadonlyTransaction(txn -> responses.get(txn, referenceAsByteArray));
		stateTime += (System.currentTimeMillis() - start);

		return responseAsByteArray == null ? Optional.empty() : Optional.of(fromByteArray(TransactionResponse::from, responseAsByteArray));
	}

	/**
	 * Yields the history of the given object, that is, the references of the transactions
	 * that provide information about the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history, if any
	 */
	Optional<Stream<TransactionReference>> getHistory(StorageReference object) {
		long start = System.currentTimeMillis();
		TransactionReference[] result = historyRecent.get(object);
		if (result != null)
			return Optional.of(Stream.of(result));

		ByteIterable historyAsByteArray = env.computeInReadonlyTransaction(txn -> history.get(txn, intoByteArray(object)));
		stateTime += (System.currentTimeMillis() - start);

		return historyAsByteArray == null ? Optional.empty() : Optional.of(Stream.of(fromByteArray(TransactionReference::from, TransactionReference[]::new, historyAsByteArray)));
	}

	/**
	 * Yields the number of commits already performed over this state.
	 * 
	 * @return the number of commits
	 */
	long getNumberOfCommits() {
		ByteIterable count = getFromInfo(COMMIT_COUNT);
		return count == null ? 0L : Long.valueOf(new String(count.getBytesUnsafe()));
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

	/**
	 * Yields the classpath of a user jar installed in blockchain, if any.
	 * This is mainly used to simplify the tests.
	 * 
	 * @return the classpath
	 */
	Optional<Classpath> getJar() {
		ByteIterable jar = getFromInfo(JAR);
		return jar == null ? Optional.empty() : Optional.of(fromByteArray(Classpath::from, jar));
	}

	/**
	 * Yields the reference that can be used for the next transaction.
	 * 
	 * @return the reference, if any
	 */
	Optional<TransactionReference> getNext() {
		ByteIterable next = getFromInfo(NEXT);
		return next == null ? Optional.empty() : Optional.of(fromByteArray(LocalTransactionReference::from, next));
	}

	/**
	 * Yields the initial accounts.
	 * 
	 * @return the accounts, as an ordered stream from the first to the last account
	 */
	Stream<StorageReference> getAccounts() {
		ByteIterable accounts = getFromInfo(ACCOUNTS);
		return accounts == null ? Stream.empty() : Stream.of(fromByteArray(StorageReference::from, StorageReference[]::new, accounts));
	}

	/**
	 * Determines if the blockchain is already initialized.
	 * 
	 * @return true if and only if {@code markAsInitialized()} has been already called
	 */
	boolean isInitialized() {
		return getFromInfo(INITIALIZED) != null;
	}

	/**
	 * Increases the number of commits performed over this state.
	 */
	private void increaseNumberOfCommits() {
		env.executeInTransaction(txn ->
			info.put(txn, COMMIT_COUNT, new ArrayByteIterable(Long.toString(getNumberOfCommits() + 1).getBytes())));
	}

	/**
	 * Yields the value of the given property in the info store.
	 * 
	 * @return true if and only if {@code markAsInitialized()} has been already called
	 */
	private ByteIterable getFromInfo(ByteIterable key) {
		long start = System.currentTimeMillis();
		return env.computeInReadonlyTransaction(txn -> {
			stateTime += (System.currentTimeMillis() - start);
			return info.get(txn, key);
		});
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

	private static ArrayByteIterable intoByteArrayWithoutSelector(StorageReference[] references) throws UncheckedIOException {
		try {
			return new ArrayByteIterable(Marshallable.toByteArrayWithoutSelector(references));
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