/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.api.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * Implementation of the shared code of a node. The goal of this class is to provide
 * some shared machinery that can be useful in subclasses.
 */
@ThreadSafe
public abstract class AbstractNodeImpl implements Node {
	protected final static Logger logger = Logger.getLogger(Node.class.getName());

	/**
	 * A map from each key of events to the subscription with this node for that key.
	 * The {@code null} key is allowed, meaning that the subscriptions are for all keys.
	 */
	private final Map<StorageReference, Set<SubscriptionImpl>> subscriptions;

	/**
	 * Builds an abstract node.
	 */
	protected AbstractNodeImpl() {
		this.subscriptions = new HashMap<>();
	}

	/**
	 * Builds a shallow clone of the given node.
	 * 
	 * @param parent the node to clone
	 */
	protected AbstractNodeImpl(AbstractNodeImpl parent) {
		this.subscriptions = parent.subscriptions;
	}

	@Override
	public final Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler) throws UnsupportedOperationException {
		Objects.requireNonNull(handler, "the handler cannot be null");

		var subscription = new SubscriptionImpl(creator, handler);

		synchronized (subscriptions) {
			subscriptions.computeIfAbsent(creator, __ -> new HashSet<>()).add(subscription);
		}

		return subscription;
	}

	/**
	 * Notifies the given event to all event handlers for the given creator.
	 * 
	 * @param creator the creator of the event
	 * @param event the event to notify
	 */
	protected final void notifyEvent(StorageReference creator, StorageReference event) {
		synchronized (subscriptions) {
			Set<SubscriptionImpl> subscriptionsPerKey = subscriptions.get(creator);
			if (subscriptionsPerKey != null)
				subscriptionsPerKey.forEach(subscription -> subscription.accept(creator, event));

			// we forward the event also to the subscriptions for all keys
			subscriptionsPerKey = subscriptions.get(null);
			if (subscriptionsPerKey != null)
				subscriptionsPerKey.forEach(subscription -> subscription.accept(creator, event));
		}

		logger.info(event + ": notified as event with creator " + creator);
	}

	/**
	 * Yields a jar supplier that polls for the outcome of a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the jar supplier
	 */
	protected final JarSupplier jarSupplierFor(TransactionReference reference) {
		return jarSupplierFor(reference, () -> getOutcomeAt((JarStoreTransactionResponse) getPolledResponse(reference), reference));
	}

	/**
	 * Yields a code supplier that polls for the outcome of a transaction that ran a constructor.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the code supplier
	 */
	protected final CodeSupplier<StorageReference> constructorSupplierFor(TransactionReference reference) {
		return codeSupplierFor(reference, () -> getOutcome((ConstructorCallTransactionResponse) getPolledResponse(reference)));
	}

	/**
	 * Yields a code supplier that polls for the outcome of a transaction that ran a method.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the code supplier
	 */
	protected final CodeSupplier<StorageValue> methodSupplierFor(TransactionReference reference) {
		return codeSupplierFor(reference, () -> getOutcome((MethodCallTransactionResponse) getPolledResponse(reference)));
	}

	protected final StorageValue getOutcome(MethodCallTransactionResponse response) throws CodeExecutionException, TransactionException {
		if (response instanceof MethodCallTransactionSuccessfulResponse mctsr)
			return mctsr.getResult();
		else if (response instanceof MethodCallTransactionExceptionResponse mcter)
			throw new CodeExecutionException(mcter.getClassNameOfCause(), mcter.getMessageOfCause(), mcter.getWhere());
		else if (response instanceof MethodCallTransactionFailedResponse mctfr)
			throw new TransactionException(mctfr.getClassNameOfCause(), mctfr.getMessageOfCause(), mctfr.getWhere());
		else
			return null; // void methods return no value
	}

	private StorageReference getOutcome(ConstructorCallTransactionResponse response) throws CodeExecutionException, TransactionException {
		if (response instanceof ConstructorCallTransactionExceptionResponse ccter)
			throw new CodeExecutionException(ccter.getClassNameOfCause(), ccter.getMessageOfCause(), ccter.getWhere());
		else if (response instanceof ConstructorCallTransactionFailedResponse cctfr)
			throw new TransactionException(cctfr.getClassNameOfCause(), cctfr.getMessageOfCause(), cctfr.getWhere());
		else
			return ((ConstructorCallTransactionSuccessfulResponse) response).getNewObject();
	}

	/**
	 * Yields the outcome of the execution having this response, performed
	 * at the given transaction reference.
	 * 
	 * @param reference the transaction reference
	 * @return the outcome
	 * @throws TransactionException if the outcome of the transaction is this exception
	 */
	private TransactionReference getOutcomeAt(JarStoreTransactionResponse response, TransactionReference reference) throws TransactionException {
		if (response instanceof JarStoreTransactionFailedResponse jstfr)
			throw new TransactionException(jstfr.getClassNameOfCause(), jstfr.getMessageOfCause(), "");
		else
			return reference;
	}
	
	/**
	 * Runs a callable and wraps any exception into an {@link TransactionRejectedException}.
	 * 
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 */
	protected static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "Unexpected exception", t);
			throw new TransactionRejectedException(t);
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
	protected static <T> T wrapInCaseOfExceptionMedium(Callable<T> what) throws TransactionRejectedException, TransactionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | TransactionException e) {
			throw e;
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "unexpected exception", t);
			throw new TransactionRejectedException(t);
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
	protected static <T> T wrapInCaseOfExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | CodeExecutionException | TransactionException e) {
			throw e;
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "unexpected exception", t);
			throw new TransactionRejectedException(t);
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

	/**
	 * An implementation of a subscription to events. It handles events
	 * with the event handler provided to the constructor and unsubscribes to events on close.
	 */
	private class SubscriptionImpl implements Subscription, BiConsumer<StorageReference, StorageReference> {
		private final StorageReference key;
		private final BiConsumer<StorageReference, StorageReference> handler;

		private SubscriptionImpl(StorageReference key, BiConsumer<StorageReference, StorageReference> handler) {
			this.key = key;
			this.handler = handler;
		}

		@Override
		public void close() {
			synchronized (subscriptions) {
				Set<SubscriptionImpl> subscriptionsForKey = subscriptions.get(key);
				if (subscriptionsForKey != null && subscriptionsForKey.remove(this) && subscriptionsForKey.isEmpty())
					subscriptions.remove(key);
			}
		}

		@Override
		public void accept(StorageReference key, StorageReference event) {
			handler.accept(key, event);
		}
	}
}