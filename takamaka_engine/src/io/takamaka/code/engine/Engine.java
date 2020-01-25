package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;

/**
 * An engine able to execute transactions on Takamaka code. Transactions are specified by a request
 * and yield a response.
 */
public interface Engine {

	/**
	 * Runs a transaction that installs a jar in this engine. The goal is to install a jar, with its dependencies.
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The engine does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public JarStoreTransactionResponse runJarStoreTransaction(JarStoreTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that calls a constructor of a class installed in the engine.
	 * The goal is to run the constructor and compute a reference to the freshly created object.
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The engine does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public ConstructorCallTransactionResponse runConstructorCallTransaction(ConstructorCallTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that calls an instance method of an object in the engine.
	 * The goal is to run the method and compute its returned value (if any).
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The engine does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public MethodCallTransactionResponse runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that calls a static method of a class in the engine.
	 * The goal is to run the method and compute its returned value (if any).
	 * This method runs the transaction specified by the request, at the given transaction reference, and yields
	 * the corresponding response. The engine does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public MethodCallTransactionResponse runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference current) throws TransactionException;
}