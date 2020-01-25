package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.takamaka.code.engine.internal.transactions.AbstractTransaction;
import io.takamaka.code.engine.internal.transactions.ConstructorCallTransactionRun;
import io.takamaka.code.engine.internal.transactions.GameteCreationTransactionRun;
import io.takamaka.code.engine.internal.transactions.JarStoreInitialTransactionRun;
import io.takamaka.code.engine.internal.transactions.RedGreenGameteCreationTransactionRun;

/**
 * A transaction of HotMoka code: it is the execution of a
 * request, that led to a response.
 *
 * @param <Request> the type of the request of this transaction
 * @param <Response> the type of the response of this transaction
 */
public interface Transaction<Request extends TransactionRequest<Response>, Response extends TransactionResponse> {

	/**
	 * The request from where this transaction started.
	 * 
	 * @return the request
	 */
	Request getRequest();

	/**
	 * The response into which this transaction led.
	 * 
	 * @return the response
	 */
	Response getResponse();

	/**
	 * Yields a transaction that installs a jar in the given node. This transaction can only occur during initialization
	 * of the node. It has no caller and requires no gas. The goal is to install, in the
	 * node, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic Takamaka classes. This method runs the transaction
	 * specified by the request, after the given transaction reference.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction after which the new transaction must be executed
	 * @param node the node that executes the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> mkFor(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new JarStoreInitialTransactionRun(request, current, node).response);
	}

	/**
	 * Yields a transaction that creates a gamete, that is, an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas. This method runs the transaction
	 * specified by the request, after the given transaction reference, and yields the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction after which the new transaction must be executed
	 * @param node the node that executes the transaction
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<GameteCreationTransactionRequest, GameteCreationTransactionResponse> mkFor(GameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new GameteCreationTransactionRun(request, current, node).response);
	}

	/**
	 * Yields a transaction that creates a red/green gamete, that is, a red/green externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas. This method runs the transaction
	 * specified by the request, after the given transaction reference, and yields the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @param node the node that executes the transaction
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> mkFor(RedGreenGameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new RedGreenGameteCreationTransactionRun(request, current, node).response);
	}

	/**
	 * Yields a transaction that calls a constructor of a class installed in the node.
	 * The goal is to run the constructor and compute a reference to the freshly created object.
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction after which this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> mkFor(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new ConstructorCallTransactionRun(request, current, node).response);
	}
}