package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
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
	private final Environment env;
    private Transaction txn;
    private Store responses;
    private Store dependencies;
    private Store history;
    private Store info;

    /**
     * The name of the store where responses of transactions are kept.
     */
    private final static String RESPONSES = "responses";

    /**
     * The name of the store where the dependencies of installed jars are kept.
     */
    private final static String DEPENDENCIES = "dependencies";

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
     * The key used inside the {@code INFO} store to keep the number of
     * commits executed with this state.
     */
    private final static ByteIterable COMMIT_COUNT = ArrayByteIterable.fromByte((byte) 0);

    /**
     * Creates a state that gets persisted inside the given directory.
     * 
     * @param dir the directory where the state is persisted
     */
    State(String dir) {
    	this.env = Environments.newInstance(dir);

    	// we enforce that all stores are created
    	env.executeInTransaction(txn -> {
    		env.openStore(RESPONSES, StoreConfig.WITHOUT_DUPLICATES, txn);
            env.openStore(DEPENDENCIES, StoreConfig.WITHOUT_DUPLICATES, txn);
            env.openStore(HISTORY, StoreConfig.WITHOUT_DUPLICATES, txn);
            env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
		});
    }

    @Override
	public void close() {
    	env.close();
	}

    /**
     * Starts a transaction. All updates during the transaction are saved
     * if the transaction will later be committed. This is called at the beginning
     * of the execution of the transactions inside a block.
     */
	void beginTransaction() {
		txn = env.beginTransaction();
        responses = env.openStore(RESPONSES, StoreConfig.WITHOUT_DUPLICATES, txn);
        dependencies = env.openStore(DEPENDENCIES, StoreConfig.WITHOUT_DUPLICATES, txn);
        history = env.openStore(HISTORY, StoreConfig.WITHOUT_DUPLICATES, txn);
        info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
	}

	/**
	 * Commits all updates during the current transaction.
	 */
	void commitTransaction() {
		increaseNumberOfCommits();
		txn.commit();
	}

	/**
	 * Puts in state the result of a transaction having the given reference.
	 * 
	 * @param transactionReference the reference of the transaction
	 * @param response the response of the transaction
	 * @throws IOException if the response cannot be saved in state
	 */
	void putResponseOf(TransactionReference transactionReference, TransactionResponse response) throws IOException {
		responses.put(txn, compactByteArraySerializationOf(transactionReference), byteArraySerializationOf(response));
	}

	void expandHistoryWith(TransactionReference transactionReference, TransactionResponseWithUpdates response) throws IOException {
		// we collect the storage references that have been updates; for each of them,
		// we fetch the list of the transaction references that affected them in the past, we add the new transaction reference
		// in front of such lists and store back the updated lists, replacing the old ones
		response.getUpdates()
			.map(Update::getObject)
			.distinct()
			.collect(Collectors.toMap
				(State::byteArraySerializationOf,
				object -> byteArraySerializationOf(getExpandedHistoryOf(object, transactionReference).toArray(TransactionReference[]::new))))
			.forEach((key, value) -> history.put(txn, key, value));
	}

	/**
	 * Puts in state the dependencies of a transaction having the given jar store request.
	 * 
	 * @param transactionReference the reference of the transaction
	 * @param request the request
	 * @throws IOException if the request cannot be saved in state
	 */
	void putDependenciesOf(TransactionReference transactionReference, AbstractJarStoreTransactionRequest request) throws IOException {
		dependencies.put(txn, compactByteArraySerializationOf(transactionReference), byteArraySerializationOf(request.getDependencies().toArray(Classpath[]::new)));
	}

	/**
	 * Yields the result of a transaction having the given reference.
	 * 
	 * @param transactionReference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponseOf(TransactionReference transactionReference) {
		return env.computeInReadonlyTransaction(txn -> {
			Store responses = env.openStore(RESPONSES, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable response = responses.get(txn, compactByteArraySerializationOf(transactionReference));
			return response == null ? Optional.empty() : Optional.of((TransactionResponse) deserializationOf(response));
		});
	}

	Optional<Stream<TransactionReference>> getHistoryOf(StorageReference object) {
		return env.computeInReadonlyTransaction(txn -> {
			Store history = env.openStore(HISTORY, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable old = history.get(txn, byteArraySerializationOf(object));
			return old == null ? Optional.empty() : Optional.of(Stream.of((TransactionReference[]) deserializationOf(old)));
		});
	}

	/**
	 * Yields the dependencies of the jar installed by a jar store transaction request having the given reference.
	 * 
	 * @param transactionReference the reference of the transaction
	 * @return the dependencies, if any
	 */
	Optional<Stream<Classpath>> getDependenciesOf(TransactionReference transactionReference) {
		return env.computeInReadonlyTransaction(txn -> {
			Store dependencies = env.openStore(DEPENDENCIES, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable response = dependencies.get(txn, compactByteArraySerializationOf(transactionReference));
			return response == null ? Optional.empty() : Optional.of(Stream.of((Classpath[]) deserializationOf(response)));
		});
	}

	long getNumberOfCommits() {
		return env.computeInReadonlyTransaction(txn -> {
			Store info = env.openStore(INFO, StoreConfig.WITHOUT_DUPLICATES, txn);
			ByteIterable count = info.get(txn, COMMIT_COUNT);
			return count == null ? 0L : (long) deserializationOf(count);
		});
	}

	private void increaseNumberOfCommits() {
		info.put(txn, COMMIT_COUNT, byteArraySerializationOf(getNumberOfCommits() + 1));
	}

	private Stream<TransactionReference> getExpandedHistoryOf(StorageReference object, TransactionReference first) {
		Optional<Stream<TransactionReference>> history = getHistoryOf(object);
		return history.isPresent() ? Stream.concat(Stream.of(first), history.get()) : Stream.of(first);
	}

	/**
	 * Yields the serialization of the given transaction reference into a byte array, that can be
	 * used in a store. This is more compact than the standard byte array serialization.
	 * 
	 * @param transactionReference the transaction reference
	 * @return the byte array
	 */
	private static ArrayByteIterable compactByteArraySerializationOf(TransactionReference transactionReference) {
		// the serialization of the toString() is shorter than the serialization of the transaction reference object
		return new ArrayByteIterable(transactionReference.toString().getBytes());
	}

	/**
	 * Serializes the given object into a byte array.
	 * 
	 * @param object the object
	 * @return the serialization of {@code object}
	 * @throws UncheckedIOException if serialization fails
	 */
	private static ArrayByteIterable byteArraySerializationOf(Serializable object) throws UncheckedIOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(object);
			return new ArrayByteIterable(baos.toByteArray());
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Object deserializationOf(ByteIterable bytes) throws UncheckedIOException {
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes.getBytesUnsafe()))) {
			return ois.readObject();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}