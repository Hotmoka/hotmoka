package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.OutOfGasError;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * A creator of a transaction. It executes a request and builds the corresponding response.
 */
public interface TransactionBuilder<Request extends TransactionRequest<Response>, Response extends TransactionResponse> {

	/**
	 * Yields the UTC time when the transaction is being run.
	 * This might be for instance the time of creation of a block where the transaction
	 * will be stored, but the detail is left to the implementation.
	 * 
	 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	long now();

	/**
	 * Yields the next storage reference for the current transaction.
	 * This can be used to associate a storage reference to each new
	 * storage object created during a transaction.
	 * 
	 * @return the next storage reference
	 */
	StorageReference getNextStorageReference();

	/**
	 * Yields the class loader used for the transaction being created.
	 * 
	 * @return the class loader
	 */
	EngineClassLoader getClassLoader();

	/**
	 * Yields the response resulting from the execution of the transaction.
	 * 
	 * @return the response
	 */
	Response getResponse();

	/**
	 * Yields the transaction reference that installed the jar where the given class is defined.
	 * 
	 * @param clazz the class, accessible during the created transaction
	 * @return the transaction reference
	 * @throws IllegalStateException if the transaction reference cannot be determined
	 */
	TransactionReference transactionThatInstalledJarFor(Class<?> clazz);

	/**
	 * Takes note of the given event, emitted during this execution.
	 * 
	 * @param event the event
	 */
	void event(Object event);

	/**
	 * Decreases the available gas by the given amount, for CPU execution.
	 * 
	 * @param amount the amount of gas to consume
	 */
	void chargeForCPU(BigInteger amount);

	/**
	 * Decreases the available gas by the given amount, for RAM execution.
	 * 
	 * @param amount the amount of gas to consume
	 */
	void chargeForRAM(BigInteger amount);

	/**
	 * Runs a given piece of code with a subset of the available gas.
	 * It first charges the given amount of gas. Then runs the code
	 * with the charged gas only. At its end, the remaining gas is added
	 * to the available gas to continue the computation.
	 * 
	 * @param amount the amount of gas provided to the code
	 * @param what the code to run
	 * @return the result of the execution of the code
	 * @throws OutOfGasError if there is not enough gas
	 * @throws Exception if the code runs into this exception
	 */
	<T> T withGas(BigInteger amount, Callable<T> what) throws Exception;
	
	/**
	 * Yields the latest value for the given field, of lazy type, of the object with the given storage reference.
	 * The field is {@code final}. Conceptually, this method looks for the value of the field
	 * in the transaction where the reference was created.
	 * 
	 * @param reference the storage reference
	 * @param field the field, of lazy type
	 * @return the value of the field
	 * @throws Exception if the look up fails
	 */
	Object deserializeLastLazyUpdateFor(StorageReference reference, FieldSignature field) throws Exception;

	/**
	 * Yields the latest value for the given field, of lazy type, of the object with the given storage reference.
	 * The field is {@code final}. Conceptually, this method looks for the value of the field
	 * in the transaction where the reference was created.
	 * 
	 * @param reference the storage reference
	 * @param field the field, of lazy type
	 * @return the value of the field
	 * @throws Exception if the look up fails
	 */
	Object deserializeLastLazyUpdateForFinal(StorageReference reference, FieldSignature field) throws Exception;
}