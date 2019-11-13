package io.takamaka.code.blockchain;

import java.util.concurrent.Callable;

import io.takamaka.code.blockchain.request.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.request.GameteCreationTransactionRequest;
import io.takamaka.code.blockchain.request.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreInitialTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.request.StaticMethodCallTransactionRequest;
import io.takamaka.code.blockchain.request.TransactionRequest;
import io.takamaka.code.blockchain.response.ConstructorCallTransactionExceptionResponse;
import io.takamaka.code.blockchain.response.ConstructorCallTransactionFailedResponse;
import io.takamaka.code.blockchain.response.ConstructorCallTransactionResponse;
import io.takamaka.code.blockchain.response.ConstructorCallTransactionSuccessfulResponse;
import io.takamaka.code.blockchain.response.JarStoreTransactionFailedResponse;
import io.takamaka.code.blockchain.response.JarStoreTransactionResponse;
import io.takamaka.code.blockchain.response.MethodCallTransactionExceptionResponse;
import io.takamaka.code.blockchain.response.MethodCallTransactionFailedResponse;
import io.takamaka.code.blockchain.response.MethodCallTransactionResponse;
import io.takamaka.code.blockchain.response.MethodCallTransactionSuccessfulResponse;
import io.takamaka.code.blockchain.response.TransactionResponse;
import io.takamaka.code.blockchain.response.VoidMethodCallTransactionSuccessfulResponse;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;

/**
 * A generic implementation of a blockchain that extends immediately when
 * a transaction request arrives. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class AbstractSequentialBlockchain extends AbstractBlockchain {

	// ABSTRACT TEMPLATE METHODS
	// Any implementation of a blockchain must implement the following and leave the rest unchanged
	
	/**
	 * Yields the reference to the transaction on top of the blockchain.
	 * If there are more chains, this refers to the transaction in the longest chain.
	 * 
	 * @return the reference to the topmost transaction, if any. Yields {@code null} if
	 *         the blockchain is empty
	 */
	protected abstract TransactionReference getTopmostTransactionReference();

	/**
	 * Expands the blockchain with a new topmost transaction. If there are more chains, this
	 * method expands the longest chain.
	 * 
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 * @return the reference to the transaction that has been added
	 * @throws Exception if the expansion cannot be completed
	 */
	protected abstract TransactionReference expandBlockchainWith(TransactionRequest request, TransactionResponse response) throws Exception;

	// BLOCKCHAIN-AGNOSTIC IMPLEMENTATION

	/**
	 * Expands this blockchain with a transaction that
	 * installs a jar in this blockchain. This transaction can only occur during initialization
	 * of the blockchain. It has no caller and requires no gas. The goal is to install, in the
	 * blockchain, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction that can be used to refer to this jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. In this case, the blockchain is not expanded
	 */
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			return expandBlockchainWith(request, runJarStoreInitialTransaction(request, getTopmostTransactionReference()));
		});
	}

	/**
	 * Expands this blockchain with a transaction that creates a gamete, that is,
	 * an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the blockchain. It has
	 * no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionException if the transaction could not be completed successfully. In this case, the blockchain is not expanded
	 */
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			return runGameteCreationTransaction(request, getTopmostTransactionReference()).gamete
				.contextualizeAt(expandBlockchainWith(request, runGameteCreationTransaction(request, getTopmostTransactionReference())));
		});
	}

	/**
	 * Expands this blockchain with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to this jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no jar will be installed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 */
	public final TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			JarStoreTransactionResponse response = runJarStoreTransaction(request, getTopmostTransactionReference());
			TransactionReference transaction = expandBlockchainWith(request, response);

			if (response instanceof JarStoreTransactionFailedResponse)
				throw ((JarStoreTransactionFailedResponse) response).cause;
			else
				return transaction;
		});
	}

	/**
	 * Expands this blockchain with a transaction that runs a constructor of a class.
	 * 
	 * @param request the request of the transaction
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.takamaka.code.blockchain.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no constructor will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the constructor is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.takamaka.code.blockchain.TransactionException} is thrown
	 */
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			ConstructorCallTransactionResponse response = runConstructorCallTransaction(request, getTopmostTransactionReference());
			TransactionReference transaction = expandBlockchainWith(request, response);

			if (response instanceof ConstructorCallTransactionFailedResponse)
				throw ((ConstructorCallTransactionFailedResponse) response).cause;
			else if (response instanceof ConstructorCallTransactionExceptionResponse)
				throw new CodeExecutionException("Constructor threw exception", ((ConstructorCallTransactionExceptionResponse) response).exception);
			else
				return ((ConstructorCallTransactionSuccessfulResponse) response).newObject.contextualizeAt(transaction);
		});
	}

	/**
	 * Runs an instance method of an object in blockchain.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.takamaka.code.blockchain.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.takamaka.code.blockchain.TransactionException} is thrown
	 */
	public final StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			MethodCallTransactionResponse response = runInstanceMethodCallTransaction(request, getTopmostTransactionReference());
			TransactionReference transaction = expandBlockchainWith(request, response);

			if (response instanceof MethodCallTransactionFailedResponse)
				throw ((MethodCallTransactionFailedResponse) response).cause;
			else if (response instanceof MethodCallTransactionExceptionResponse)
				throw new CodeExecutionException("Method threw exception", ((MethodCallTransactionExceptionResponse) response).exception);
			else if (response instanceof VoidMethodCallTransactionSuccessfulResponse)
				return null;
			else {
				StorageValue result = ((MethodCallTransactionSuccessfulResponse) response).result;
				return result instanceof StorageReference ?
					((StorageReference) result).contextualizeAt(transaction) : result;
			}
		});
	}

	/**
	 * Expands this blockchain with a transaction that runs a static method of a class in blockchain.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.takamaka.code.blockchain.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.takamaka.code.blockchain.TransactionException} is thrown
	 */
	public final StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			MethodCallTransactionResponse response = runStaticMethodCallTransaction(request, getTopmostTransactionReference());
			TransactionReference transaction = expandBlockchainWith(request, response);

			if (response instanceof MethodCallTransactionFailedResponse)
				throw ((MethodCallTransactionFailedResponse) response).cause;
			else if (response instanceof MethodCallTransactionExceptionResponse)
				throw new CodeExecutionException("Method threw exception", ((MethodCallTransactionExceptionResponse) response).exception);
			else if (response instanceof VoidMethodCallTransactionSuccessfulResponse)
				return null;
			else {
				StorageValue result = ((MethodCallTransactionSuccessfulResponse) response).result;
				return result instanceof StorageReference ?
					((StorageReference) result).contextualizeAt(transaction) : result;
			}
		});
	}

	/**
	 * Calls the given callable. If if throws an exception, it wraps into into a {@link io.takamaka.code.blockchain.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws TransactionException the wrapped exception
	 */
	private static <T> T wrapInCaseOfException(Callable<T> what) throws TransactionException {
		try {
			return what.call();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}
	}

	/**
	 * Calls the given callable. If if throws a {@link io.takamaka.code.blockchain.CodeExecutionException}, if throws it back
	 * unchanged. Otherwise, it wraps the exception into into a {@link io.takamaka.code.blockchain.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws CodeExecutionException the unwrapped exception
	 * @throws TransactionException the wrapped exception
	 */
	private static <T> T wrapWithCodeInCaseOfException(Callable<T> what) throws TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (CodeExecutionException e) {
			throw e;
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}
	}

	/**
	 * Wraps the given throwable in a {@link io.takamaka.code.blockchain.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @param message the message added to the {@link io.takamaka.code.blockchain.TransactionException}, if wrapping occurs
	 * @return the wrapped or original exception
	 */
	private static TransactionException wrapAsTransactionException(Throwable t, String message) {
		if (t instanceof TransactionException)
			return (TransactionException) t;
		else
			return new TransactionException(message, t);
	}
}