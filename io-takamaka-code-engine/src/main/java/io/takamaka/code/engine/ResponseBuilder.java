package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionRejectedException;
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
import io.takamaka.code.engine.internal.transactions.ConstructorCallResponseBuilder;
import io.takamaka.code.engine.internal.transactions.GameteCreationResponseBuilder;
import io.takamaka.code.engine.internal.transactions.InstanceMethodCallResponseBuilder;
import io.takamaka.code.engine.internal.transactions.InstanceViewMethodCallResponseBuilder;
import io.takamaka.code.engine.internal.transactions.JarStoreInitialResponseBuilder;
import io.takamaka.code.engine.internal.transactions.JarStoreResponseBuilder;
import io.takamaka.code.engine.internal.transactions.RedGreenGameteCreationResponseBuilder;
import io.takamaka.code.engine.internal.transactions.StaticMethodCallResponseBuilder;
import io.takamaka.code.engine.internal.transactions.StaticViewMethodCallResponseBuilder;

/**
 * The creator of a response from a request. It executes a transaction from the request and builds the corresponding response.
 * The factory methods in this interface check the prerequisite for running the
 * transaction, such as the fact that the caller can be identified and has provided a minimum of gas.
 * The {@linkplain #build()} method, instead, performs the actual creation of the response.
 * If the factory methods fail, then a node could for instance reject the request.
 * 
 * @param <Response> the type of the response of the transaction
 */
public interface ResponseBuilder<Response extends TransactionResponse> {

	/**
	 * Builds the response of the transaction.
	 * 
	 * @param current the reference that can be used to refer to the transaction
	 * @return the response
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	Response build(TransactionReference current) throws TransactionRejectedException;

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that installs a jar in the given node.
	 * This transaction can only occur during initialization of the node. It has no caller
	 * and requires no gas. The goal is to install, in the node, some basic jars that are
	 * likely needed as dependencies by future jars. For instance, the jar containing the
	 * basic Takamaka classes.
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<JarStoreInitialTransactionResponse> of(JarStoreInitialTransactionRequest request, Node node) throws TransactionRejectedException {
		return new JarStoreInitialResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that creates a gamete, that is, an externally
	 * owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas.
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<GameteCreationTransactionResponse> of(GameteCreationTransactionRequest request, Node node) throws TransactionRejectedException {
		return new GameteCreationResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that creates a red/green gamete, that is, a red/green externally owned contract
	 * with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas.
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<GameteCreationTransactionResponse> of(RedGreenGameteCreationTransactionRequest request, Node node) throws TransactionRejectedException {
		return new RedGreenGameteCreationResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that installs a jar in this node. The goal is to install a jar, with its dependencies.
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<JarStoreTransactionResponse> of(JarStoreTransactionRequest request, Node node) throws TransactionRejectedException {
		return new JarStoreResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls a constructor of a class installed in the node.
	 * The goal is to run the constructor and compute a reference to the freshly created object.
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<ConstructorCallTransactionResponse> of(ConstructorCallTransactionRequest request, Node node) throws TransactionRejectedException {
		return new ConstructorCallResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls an instance method of an object in the node.
	 * The goal is to run the method and compute its returned value (if any).
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<MethodCallTransactionResponse> of(InstanceMethodCallTransactionRequest request, Node node) throws TransactionRejectedException {
		return new InstanceMethodCallResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls a static method. The goal is to run the method and compute its returned value (if any).
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<MethodCallTransactionResponse> of(StaticMethodCallTransactionRequest request, Node node) throws TransactionRejectedException {
		return new StaticMethodCallResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls an instance method of an object in the node.
	 * The goal is to run the method and compute its returned value (if any).
	 * The method must be annotated as {@linkplain io.hotmoka.code.lang.View}.
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<MethodCallTransactionResponse> ofView(InstanceMethodCallTransactionRequest request, Node node) throws TransactionRejectedException {
		return new InstanceViewMethodCallResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls a static method. The goal is to run the method and compute its returned value (if any).
	 * The method must be annotated as {@linkplain io.hotmoka.code.lang.View}.
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<MethodCallTransactionResponse> ofView(StaticMethodCallTransactionRequest request, Node node) throws TransactionRejectedException {
		return new StaticViewMethodCallResponseBuilder(request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction.
	 * It forwards the call to any of the most specified {@code of} methods.
	 * 
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<?> of(TransactionRequest<?> request, Node node) throws TransactionRejectedException {
		if (request instanceof JarStoreInitialTransactionRequest)
			return of((JarStoreInitialTransactionRequest) request, node);
		else if (request instanceof RedGreenGameteCreationTransactionRequest)
			return of((RedGreenGameteCreationTransactionRequest) request, node);
    	else if (request instanceof GameteCreationTransactionRequest)
    		return of((GameteCreationTransactionRequest) request, node);
    	else if (request instanceof JarStoreTransactionRequest)
    		return of((JarStoreTransactionRequest) request, node);
    	else if (request instanceof ConstructorCallTransactionRequest)
    		return of((ConstructorCallTransactionRequest) request, node);
    	else if (request instanceof InstanceMethodCallTransactionRequest)
    		return of((InstanceMethodCallTransactionRequest) request, node);
    	else if (request instanceof StaticMethodCallTransactionRequest)
    		return of((StaticMethodCallTransactionRequest) request, node);
    	else
    		throw new TransactionRejectedException("unexpected transaction request of class " + request.getClass().getName());
	}
}