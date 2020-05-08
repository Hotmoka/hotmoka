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
 * The factory methods can be executed in a thread-safe way, since they do not depend on
 * information in the node's store that might be modified by other transactions.
 * If these factory methods fail, then a node could for instance reject the request.
 * The {@linkplain #build(TransactionReference)} method performs the actual creation of the response and
 * depends on the current store of the node. Hence, {@linkplain #build(TransactionReference)}
 * is not thread-safe and should be executed only after a lock is taken on the store of the node.
 * 
 * @param <Response> the type of the response of the transaction
 */
public interface ResponseBuilder<Request extends TransactionRequest<Response>, Response extends TransactionResponse> {

	/**
	 * Builds the response of the transaction.
	 * 
	 * @return the response
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	Response build() throws TransactionRejectedException;

	/**
	 * Yield the request for which this builder was created.
	 * 
	 * @return the request
	 */
	Request getRequest();

	/**
	 * Yield the reference to the transaction for the request.
	 * 
	 * @return the reference to the transaction
	 */
	TransactionReference getTransaction();

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that installs a jar in the given node.
	 * This transaction can only occur during initialization of the node. It has no caller
	 * and requires no gas. The goal is to install, in the node, some basic jars that are
	 * likely needed as dependencies by future jars. For instance, the jar containing the
	 * basic Takamaka classes.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> of(TransactionReference reference, JarStoreInitialTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new JarStoreInitialResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that creates a gamete, that is, an externally
	 * owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<GameteCreationTransactionRequest, GameteCreationTransactionResponse> of(TransactionReference reference, GameteCreationTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new GameteCreationResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that creates a red/green gamete, that is, a red/green externally owned contract
	 * with the given initial amount of coins.
	 * This transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> of(TransactionReference reference, RedGreenGameteCreationTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new RedGreenGameteCreationResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that installs a jar in this node. The goal is to install a jar, with its dependencies.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<JarStoreTransactionRequest, JarStoreTransactionResponse> of(TransactionReference reference, JarStoreTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new JarStoreResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls a constructor of a class installed in the node.
	 * The goal is to run the constructor and compute a reference to the freshly created object.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> of(TransactionReference reference, ConstructorCallTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new ConstructorCallResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls an instance method of an object in the node.
	 * The goal is to run the method and compute its returned value (if any).
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> of(TransactionReference reference, InstanceMethodCallTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new InstanceMethodCallResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls a static method. The goal is to run the method and compute its returned value (if any).
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<StaticMethodCallTransactionRequest, MethodCallTransactionResponse> of(TransactionReference reference, StaticMethodCallTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new StaticMethodCallResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls an instance method of an object in the node.
	 * The goal is to run the method and compute its returned value (if any).
	 * The method must be annotated as {@linkplain io.hotmoka.code.lang.View}.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> ofView(TransactionReference reference, InstanceMethodCallTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new InstanceViewMethodCallResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction
	 * that calls a static method. The goal is to run the method and compute its returned value (if any).
	 * The method must be annotated as {@linkplain io.hotmoka.code.lang.View}.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<StaticMethodCallTransactionRequest, MethodCallTransactionResponse> ofView(TransactionReference reference, StaticMethodCallTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		return new StaticViewMethodCallResponseBuilder(reference, request, node);
	}

	/**
	 * Yields the builder of a response for a request of a transaction.
	 * It forwards the call to any of the most specified {@code of} methods.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @param node the node that executes the transaction
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	static ResponseBuilder<?,?> of(TransactionReference reference, TransactionRequest<?> request, AbstractNode<?> node) throws TransactionRejectedException {
		if (request instanceof JarStoreInitialTransactionRequest)
			return of(reference, (JarStoreInitialTransactionRequest) request, node);
		else if (request instanceof RedGreenGameteCreationTransactionRequest)
			return of(reference, (RedGreenGameteCreationTransactionRequest) request, node);
    	else if (request instanceof GameteCreationTransactionRequest)
    		return of(reference, (GameteCreationTransactionRequest) request, node);
    	else if (request instanceof JarStoreTransactionRequest)
    		return of(reference, (JarStoreTransactionRequest) request, node);
    	else if (request instanceof ConstructorCallTransactionRequest)
    		return of(reference, (ConstructorCallTransactionRequest) request, node);
    	else if (request instanceof InstanceMethodCallTransactionRequest)
    		return of(reference, (InstanceMethodCallTransactionRequest) request, node);
    	else if (request instanceof StaticMethodCallTransactionRequest)
    		return of(reference, (StaticMethodCallTransactionRequest) request, node);
    	else
    		throw new TransactionRejectedException("unexpected transaction request of class " + request.getClass().getName());
	}
}