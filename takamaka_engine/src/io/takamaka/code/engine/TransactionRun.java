package io.takamaka.code.engine;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.values.StorageReference;

/**
 * A creator of a transaction. It executes a request and builds the corresponding response.
 */
public interface TransactionRun {

	/**
	 * Yields the reference to the transaction being executed.
	 * 
	 * @return the reference
	 */
	TransactionReference getCurrentTransaction();

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
}