package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.transactions.AbstractTransaction;
import io.takamaka.code.engine.internal.transactions.ConstructorCallTransactionBuilder;
import io.takamaka.code.engine.internal.transactions.GameteCreationTransactionBuilder;
import io.takamaka.code.engine.internal.transactions.InstanceMethodCallTransactionBuilder;
import io.takamaka.code.engine.internal.transactions.InstanceViewMethodCallTransactionBuilder;
import io.takamaka.code.engine.internal.transactions.JarStoreInitialTransactionBuilder;
import io.takamaka.code.engine.internal.transactions.JarStoreTransactionBuilder;
import io.takamaka.code.engine.internal.transactions.RedGreenGameteCreationTransactionBuilder;
import io.takamaka.code.engine.internal.transactions.StaticMethodCallTransactionBuilder;
import io.takamaka.code.engine.internal.transactions.StaticViewMethodCallTransactionBuilder;

/**
 * A transaction of a HotMoka node: it is the execution of a
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
	 * For instance, the jar containing the basic Takamaka classes. This method runs the transaction specified by the request.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @param node the node that executes the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> mkFor(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new JarStoreInitialTransactionBuilder(request, current, node).getResponse());
	}

	/**
	 * Yields a transaction that creates a gamete, that is, an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas. This method runs the transaction
	 * specified by the request and yields the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @param node the node that executes the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<GameteCreationTransactionRequest, GameteCreationTransactionResponse> mkFor(GameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new GameteCreationTransactionBuilder(request, current, node).getResponse());
	}

	/**
	 * Yields a transaction that creates a red/green gamete, that is, a red/green externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas. This method runs the transaction
	 * specified by the request and yields the corresponding transaction.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @param node the node that executes the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> mkFor(RedGreenGameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new RedGreenGameteCreationTransactionBuilder(request, current, node).getResponse());
	}

	/**
	 * Yields a transaction that installs a jar in this engine. The goal is to install a jar, with its dependencies.
	 * This method runs the transaction specified by the request and yields the corresponding transaction.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<JarStoreTransactionRequest, JarStoreTransactionResponse> mkFor(JarStoreTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new JarStoreTransactionBuilder(request, current, node).getResponse());
	}

	/**
	 * Yields a transaction that calls a constructor of a class installed in the node.
	 * The goal is to run the constructor and compute a reference to the freshly created object.
	 * This method runs the transaction specified by the request and yields the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> mkFor(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new ConstructorCallTransactionBuilder(request, current, node).getResponse());
	}

	/**
	 * Yields a transaction that calls an instance method of an object in the node.
	 * The goal is to run the method and compute its returned value (if any).
	 * This method runs the transaction specified by the request and yields the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> mkFor(InstanceMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new InstanceMethodCallTransactionBuilder(request, current, node).getResponse());
	}

	/**
	 * Yields a transaction that calls a static method.
	 * The goal is to run the method and compute its returned value (if any).
	 * This method runs the transaction specified by the request and yields the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<StaticMethodCallTransactionRequest, MethodCallTransactionResponse> mkFor(StaticMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new StaticMethodCallTransactionBuilder(request, current, node).getResponse());
	}

	/**
	 * Yields a transaction that calls an instance method of an object in the node.
	 * The goal is to run the method and compute its returned value (if any).
	 * The method is checked to be annotated as {@linkplain io.hotmoka.code.lang.View}.
	 * This method runs the transaction specified by the request and yields the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> mkForView(InstanceMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new InstanceViewMethodCallTransactionBuilder(request, current, node).getResponse());
	}

	/**
	 * Yields a transaction that calls a static method.
	 * The goal is to run the method and compute its returned value (if any).
	 * The method is checked to be annotated as {@linkplain io.hotmoka.code.lang.View}.
	 * This method runs the transaction specified by the request and yields the corresponding response.
	 * 
	 * @param request the transaction request
	 * @param current the reference that will be used for the transaction
	 * @return the transaction
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	static Transaction<StaticMethodCallTransactionRequest, MethodCallTransactionResponse> mkForView(StaticMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new AbstractTransaction<>(request, new StaticViewMethodCallTransactionBuilder(request, current, node).getResponse());
	}
}