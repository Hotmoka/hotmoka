package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.takamaka.code.engine.internal.transactions.AbstractTransaction;
import io.takamaka.code.engine.internal.transactions.JarStoreInitialTransaction;

/**
 * A transaction of HotMoka code: it is the execution of a
 * request, that led to a response.
 *
 * @param <R> the type of the response of this transaction
 */
public interface Transaction<Request extends TransactionRequest<Response>, Response extends TransactionResponse> {

	/**
	 * The request from where this transaction started
	 * 
	 * @return the request
	 */
	Request getRequest();

	/**
	 * The response into which this transaction led
	 * 
	 * @return the response
	 */
	Response getResponse();

	/**
	 * Computes a transaction that installs a jar. This transaction can only occur during initialization
	 * of the engine. It has no caller and requires no gas. The goal is to install, in the
	 * engine, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes. This method runs the transaction
	 * specified by the request, after the given transaction reference, and yields the corresponding response.
	 * The engine does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static AbstractTransaction<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> mkFor(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new JarStoreInitialTransaction(request, current, node);
	}
}