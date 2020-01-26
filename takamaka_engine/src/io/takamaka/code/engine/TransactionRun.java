package io.takamaka.code.engine;

import java.math.BigInteger;
import java.util.SortedSet;
import java.util.concurrent.Callable;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.engine.internal.Deserializer;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.engine.internal.StorageTypeToClass;

/**
 * A creator of a transaction. It executes a request and builds the corresponding response.
 */
public interface TransactionRun {

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
	 * Yields the class loader used for running the transaction being built.
	 * 
	 * @return the class loader
	 */
	EngineClassLoader getClassLoader();

	/**
	 * Yields the object that translates storage types into class types, for the
	 * transaction being built.
	 * 
	 * @return the object
	 */
	StorageTypeToClass getStorageTypeToClass();

	/**
	 * Yields the deserializer for the transaction being built.
	 * 
	 * @return the deserializer
	 */
	Deserializer getDeserializer();

	Node getNode();

	/**
	 * Yields the UTC time when the transaction is being run.
	 * This might be for instance the time of creation of the block where the transaction
	 * occurs, but the detail is left to the implementation.
	 * 
	 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	long now();

	/**
	 * Collects all updates reachable from the actuals or from the caller, receiver or result of a method call.
	 * 
	 * @param actuals the actuals; only {@code Storage} are relevant; this might be {@code null}
	 * @param caller the caller of an {@code @@Entry} method; this might be {@code null}
	 * @param receiver the receiver of the call; this might be {@code null}
	 * @param result the result; relevant only if {@code Storage}
	 * @return the ordered updates
	 */
	SortedSet<Update> collectUpdates(Object[] actuals, Object caller, Object receiver, Object result);

	/**
	 * Yields the reference to the transaction being executed.
	 * 
	 * @return the reference
	 */
	TransactionReference getCurrentTransaction();

	/**
	 * Yields the latest value for the given field, of lazy type, of the given storage reference.
	 * The field is not {@code final}.
	 * Conceptually, this method goes backwards from the tip of the blockchain, looking for the latest
	 * update of the given field.
	 * 
	 * @param reference the storage reference
	 * @param field the field, of lazy type
	 * @return the value of the field
	 * @throws Exception if the look up fails
	 */
	Object deserializeLastLazyUpdateFor(StorageReference reference, FieldSignature field, TransactionRun run) throws Exception;

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
	Object deserializeLastLazyUpdateForFinal(StorageReference reference, FieldSignature field, TransactionRun run) throws Exception;

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
	 * Adds an event to those occurred during the execution of the current transaction.
	 * 
	 * @param event the event
	 * @throws IllegalArgumentException if the event is {@code null}
	 */
	void event(Object event);
}