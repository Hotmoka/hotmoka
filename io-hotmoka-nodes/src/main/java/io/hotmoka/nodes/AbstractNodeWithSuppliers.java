package io.hotmoka.nodes;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A node with some methods for building code and jar suppliers.
 * The goal of this class is to provide some shared machinery that can
 * be useful in subclasses.
 */
public abstract class AbstractNodeWithSuppliers implements Node {
	protected final static Logger logger = LoggerFactory.getLogger(Node.class);

	/**
	 * Yields a jar supplier that polls for the outcome of a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the jar supplier
	 */
	protected final JarSupplier jarSupplierFor(TransactionReference reference) {
		return jarSupplierFor(reference, () -> ((JarStoreTransactionResponse) getPolledResponseAt(reference)).getOutcomeAt(reference));
	}

	/**
	 * Yields a code supplier that polls for the outcome of a transaction that ran a constructor.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the code supplier
	 */
	protected final CodeSupplier<StorageReference> constructorSupplierFor(TransactionReference reference) {
		return codeSupplierFor(reference, () -> ((ConstructorCallTransactionResponse) getPolledResponseAt(reference)).getOutcome());
	}

	/**
	 * Yields a code supplier that polls for the outcome of a transaction that ran a method.
	 * 
	 * @param reference the reference of the request of the transaction
	 * @return the code supplier
	 */
	protected final CodeSupplier<StorageValue> methodSupplierFor(TransactionReference reference) {
		return codeSupplierFor(reference, () -> ((MethodCallTransactionResponse) getPolledResponseAt(reference)).getOutcome());
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
		logger.error("transaction rejected", cause);
		return new TransactionRejectedException(cause);
	}
}