package io.takamaka.code.engine;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;

/**
 * A creator of a transaction. It executes a request and builds the corresponding response.
 */
public interface TransactionBuilder {

	/**
	 * Yields the UTC time when the transaction is being run.
	 * This might be for instance the time of creation of the block where the transaction
	 * occurs, but the detail is left to the implementation.
	 * 
	 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	long now();

	/**
	 * Yields the next storage reference for the current transaction.
	 * 
	 * @return the next storage reference
	 */
	StorageReference getNextStorageReference();

	/**
	 * Yields the node for which the transaction is being run.
	 * 
	 * @return the node
	 */
	Node getNode();

	/**
	 * Yields the value of the {@code storageReference} field
	 * of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	StorageReference getStorageReferenceOf(Object object);

	/**
	 * Yields the value of the boolean {@code inStorage} field
	 * of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	boolean getInStorageOf(Object object);

	/**
	 * Called at the beginning of the instrumentation of an entry method or constructor
	 * of a contract. It forwards the call to {@code io.takamaka.code.lang.Contract.entry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.entry()}
	 */
	void entry(Object callee, Object caller) throws Throwable;

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	void payableEntry(Object callee, Object caller, BigInteger amount) throws Throwable;

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	void redPayableEntry(Object callee, Object caller, BigInteger amount) throws Throwable;

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.entry()}
	 */
	void payableEntry(Object callee, Object caller, int amount) throws Throwable;

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	void redPayableEntry(Object callee, Object caller, int amount) throws Throwable;

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.entry()}
	 */
	void payableEntry(Object callee, Object caller, long amount) throws Throwable;

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	void redPayableEntry(Object callee, Object caller, long amount) throws Throwable;

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
	 * Decreases the available gas by the given amount, for storage allocation.
	 * 
	 * @param amount the amount of gas to consume
	 */
	void chargeForStorage(BigInteger amount);

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
	 * Yields the latest value for the given field, of lazy type, of the given storage reference.
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
	 * Yields the latest value for the given field, of lazy type, of the given storage reference.
	 * The field is {@code final}. Conceptually, this method looks for the value of the field
	 * in the transaction where the reference was created.
	 * 
	 * @param reference the storage reference
	 * @param field the field, of lazy type
	 * @return the value of the field
	 * @throws Exception if the look up fails
	 */
	Object deserializeLastLazyUpdateForFinal(StorageReference reference, FieldSignature field) throws Exception;

	/**
	 * Collects all eager fields of the given storage class, including those of its superclasses,
	 * up to and excluding {@link io.takamaka.code.lang.Storage}.
	 * 
	 * @param className the name of the storage class
	 * @return the eager fields
	 * @throws ClassNotFoundException if some class could not be found
	 */
	Stream<Field> collectEagerFieldsOf(String className) throws ClassNotFoundException;
}