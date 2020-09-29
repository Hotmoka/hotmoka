package io.hotmoka.nodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A generic implementation of a node. The goal of this class is to provide
 * some shared machinery that can be useful in subclasses.
 */
@ThreadSafe
public abstract class AbstractNode implements Node {
	protected final static Logger logger = LoggerFactory.getLogger(Node.class);

	/**
	 * A map from each key of events to the event handlers subscribed with this node for that key.
	 */
	private final Map<StorageReference, Set<Consumer<StorageReference>>> eventHandlers = new HashMap<>();

	@Override
	public final void subscribeToEvents(StorageReference key, Consumer<StorageReference> handler) throws UnsupportedOperationException {
		if (key == null)
			throw new NullPointerException("the key cannot be null");

		if (handler == null)
			throw new NullPointerException("the handler cannot be null");

		synchronized (eventHandlers) {
			eventHandlers.computeIfAbsent(key, __ -> new HashSet<>()).add(handler);
		}
	}

	@Override
	public final void unsubscribeToEvents(StorageReference key, Consumer<StorageReference> handler) throws UnsupportedOperationException {
		if (key == null)
			throw new NullPointerException("the key cannot be null");

		if (handler == null)
			throw new NullPointerException("the handler cannot be null");

		synchronized (eventHandlers) {
			Set<Consumer<StorageReference>> handlers = eventHandlers.get(key);
			if (handlers != null && handlers.remove(handler) && handlers.isEmpty())
				eventHandlers.remove(key);
		}
	}

	/**
	 * Notifies the given event to all event handlers for the given key.
	 * 
	 * @param key the key of the event
	 * @param event the event to notify
	 */
	protected final void notifyEvent(StorageReference key, StorageReference event) {
		try {
			synchronized (eventHandlers) {
				Set<Consumer<StorageReference>> handlers = eventHandlers.get(key);
				if (handlers != null)
					handlers.forEach(handler -> handler.accept(event));
			}

			logger.info(event + ": notified as event with key " + key);
		}
		catch (Throwable t) {
			throw InternalFailureException.of("event handler execution failed", t);
		}
	}

	/**
	 * Yields a jar supplier that polls for the outcome of a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the jar supplier
	 */
	protected final JarSupplier jarSupplierFor(TransactionReference reference) {
		return jarSupplierFor(reference, () -> ((JarStoreTransactionResponse) getPolledResponse(reference)).getOutcomeAt(reference));
	}

	/**
	 * Yields a code supplier that polls for the outcome of a transaction that ran a constructor.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the code supplier
	 */
	protected final CodeSupplier<StorageReference> constructorSupplierFor(TransactionReference reference) {
		return codeSupplierFor(reference, () -> ((ConstructorCallTransactionResponse) getPolledResponse(reference)).getOutcome());
	}

	/**
	 * Yields a code supplier that polls for the outcome of a transaction that ran a method.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the code supplier
	 */
	protected final CodeSupplier<StorageValue> methodSupplierFor(TransactionReference reference) {
		return codeSupplierFor(reference, () -> ((MethodCallTransactionResponse) getPolledResponse(reference)).getOutcome());
	}

	/**
	 * Runs a callable and wraps any exception into an {@link TransactionRejectedException}.
	 * 
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 */
	protected final static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null)
				throw rejectTransaction(e.getCause());
	
			throw rejectTransaction(e);
		}
		catch (Throwable t) {
			throw rejectTransaction(t);
		}
	}

	/**
	 * Runs a callable and wraps any exception into an {@link TransactionRejectedException},
	 * if it is not a {@link TransactionException}.
	 * 
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 * @throws TransactionException if the callable throws this
	 */
	protected final static <T> T wrapInCaseOfExceptionMedium(Callable<T> what) throws TransactionRejectedException, TransactionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | TransactionException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null)
				throw rejectTransaction(e.getCause());
	
			throw rejectTransaction(e);
		}
		catch (Throwable t) {
			throw rejectTransaction(t);
		}
	}

	/**
	 * Runs a callable and wraps any exception into an {@link TransactionRejectedException},
	 * if it is not a {@link TransactionException} nor a {@link CodeExecutionException}.
	 * 
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 * @throws TransactionException if the callable throws this
	 * @throws CodeExecutionException if the callable throws this
	 */
	protected final static <T> T wrapInCaseOfExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | CodeExecutionException | TransactionException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null)
				throw rejectTransaction(e.getCause());
	
			throw rejectTransaction(e);
		}
		catch (Throwable t) {
			throw rejectTransaction(t);
		}
	}

	/**
	 * Adapts a callable into a jar supplier.
	 * 
	 * @param reference the reference of the request whose future is being built
	 * @param task the callable
	 * @return the jar supplier
	 */
	private static JarSupplier jarSupplierFor(TransactionReference reference, Callable<TransactionReference> task) {
		return new JarSupplier() {
			private volatile TransactionReference cachedGet;
	
			@Override
			public TransactionReference getReferenceOfRequest() {
				return reference;
			}
	
			@Override
			public TransactionReference get() throws TransactionRejectedException, TransactionException {
				return cachedGet != null ? cachedGet : (cachedGet = wrapInCaseOfExceptionMedium(task));
			}
		};
	}

	/**
	 * Adapts a callable into a code supplier.
	 * 
	 * @param <W> the return value of the callable
	 * @param reference the reference of the request whose future is being built
	 * @param task the callable
	 * @return the code supplier
	 */
	private static <W extends StorageValue> CodeSupplier<W> codeSupplierFor(TransactionReference reference, Callable<W> task) {
		return new CodeSupplier<>() {
			private volatile W cachedGet;
	
			@Override
			public TransactionReference getReferenceOfRequest() {
				return reference;
			}
	
			@Override
			public W get() throws TransactionRejectedException, TransactionException, CodeExecutionException {
				return cachedGet != null ? cachedGet : (cachedGet = wrapInCaseOfExceptionFull(task));
			}
		};
	}

	private static TransactionRejectedException rejectTransaction(Throwable cause) throws TransactionRejectedException {
		return new TransactionRejectedException(cause);
	}
}