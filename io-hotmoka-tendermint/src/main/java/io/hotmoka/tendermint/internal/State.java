package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.AbstractJarStoreTransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
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

    /**
     * The name of the store where responses of transactions are kept.
     */
    private final static String RESPONSES = "responses";

    /**
     * The name of the store where the dependencies of installed jars are kept.
     */
    private final static String DEPENDENCIES = "dependencies";

    State() {
    	this.env = Environments.newInstance("tmp/storage");
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
	}

	/**
	 * Commits all updates during the current transaction.
	 */
	void commitTransaction() {
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
		responses.put(txn, transactionReferenceAsKey(transactionReference), byteArraySerializationOf(response));
	}

	/**
	 * Puts in state the dependencies of a transaction having the given jar store request.
	 * 
	 * @param transactionReference the reference of the transaction
	 * @param request the request
	 * @throws IOException if the request cannot be saved in state
	 */
	void putDependenciesOf(TransactionReference transactionReference, AbstractJarStoreTransactionRequest request) throws IOException {
		dependencies.put(txn, transactionReferenceAsKey(transactionReference), byteArraySerializationOf(request.getDependencies().toArray(Classpath[]::new)));
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
			ByteIterable response = responses.get(txn, transactionReferenceAsKey(transactionReference));
			if (response == null)
				return Optional.empty();
	
			try {
				return Optional.of((TransactionResponse) deserializationOf(response));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
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
			ByteIterable response = dependencies.get(txn, transactionReferenceAsKey(transactionReference));
			if (response == null)
				return Optional.empty();

			try {
				return Optional.of(Stream.of((Classpath[]) deserializationOf(response)));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Yields the serialization of the given transaction reference into a byte array, that can be
	 * used as key for a store.
	 * 
	 * @param transactionReference the transaction reference
	 * @return the byte array
	 */
	private static ArrayByteIterable transactionReferenceAsKey(TransactionReference transactionReference) {
		// the serialization of the toString() is shorter than the serialization of the transaction reference object
		return new ArrayByteIterable(transactionReference.toString().getBytes());
	}

	/**
	 * Serializes the given object into a byte array.
	 * 
	 * @param object the object
	 * @return the serialization of {@code object}
	 * @throws IOException if serialization fails
	 */
	private static ArrayByteIterable byteArraySerializationOf(Serializable object) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(object);
			return new ArrayByteIterable(baos.toByteArray());
		}
	}

	private static Object deserializationOf(ByteIterable bytes) throws IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes.getBytesUnsafe()))) {
			return ois.readObject();
		}
	}
}