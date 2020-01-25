package io.takamaka.code.engine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * A generic implementation of a blockchain that extends immediately when
 * a transaction request arrives. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class AbstractSequentialBlockchain extends AbstractBlockchain {

	/**
	 * Yields the reference to the transaction on top of the blockchain.
	 * If there are more chains, this refers to the transaction in the longest chain.
	 * 
	 * @return the reference to the topmost transaction, if any. Yields {@code null} if
	 *         the blockchain is empty
	 */
	protected abstract SequentialTransactionReference getTopmostTransactionReference();

	/**
	 * Yields the reference to the transaction that follows the topmost one.
	 * If there are more chains, this refers to the transaction in the longest chain.
	 * 
	 * @return the reference to the next transaction
	 */
	protected abstract SequentialTransactionReference getNextTransaction();

	/**
	 * Expands the blockchain with a new topmost transaction. If there are more chains, this
	 * method expands the longest chain.
	 * 
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 * @return the reference to the transaction that has been added
	 * @throws Exception if the expansion cannot be completed
	 */
	protected abstract <R extends TransactionResponse> TransactionReference expandBlockchainWith(TransactionRequest<? extends R> request, R response) throws Exception;

	@Override
	public Stream<Update> getLastEagerUpdatesFor(StorageReference reference, TransactionRun run) throws Exception {
		TransactionReference transaction = reference.transaction;
	
		TransactionResponse response = getResponseAtAndCharge(transaction, run);
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new DeserializationError("Storage reference " + reference + " does not contain updates");
	
		Set<Update> updates = ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update.object.equals(reference) && update.isEager())
				.collect(Collectors.toSet());
	
		Optional<ClassTag> classTag = updates.stream()
				.filter(update -> update instanceof ClassTag)
				.map(update -> (ClassTag) update)
				.findAny();
	
		if (!classTag.isPresent())
			throw new DeserializationError("No class tag found for " + reference);
	
		// we drop updates to non-final fields
		Set<Field> eagerFields = collectEagerFieldsOf(classTag.get().className, run);
		Iterator<Update> it = updates.iterator();
		while (it.hasNext())
			if (updatesNonFinalField(it.next(), eagerFields, run))
				it.remove();
	
		// the updates set contains the updates to eager final fields now:
		// we must still collect the latest updates to the eager non-final fields
		return collectEagerUpdatesFor(reference, updates, eagerFields.size(), run);
	}

	@Override
	public UpdateOfField getLastLazyUpdateToNonFinalFieldOf(StorageReference object, FieldSignature field, TransactionRun run) throws Exception {
		// goes back from the previous transaction;
		// there is no reason to look before the transaction that created the object
		for (SequentialTransactionReference cursor = getTopmostTransactionReference(); !cursor.isOlderThan(object.transaction); cursor = cursor.getPrevious()) {
			Optional<UpdateOfField> update = getLastUpdateFor(object, field, cursor, run);
			if (update.isPresent())
				return update.get();
		}
	
		throw new DeserializationError("Did not find the last update for " + field + " of " + object);
	}

	@Override
	public UpdateOfField getLastLazyUpdateToFinalFieldOf(StorageReference object, FieldSignature field, TransactionRun run) throws Exception {
		// goes directly to the transaction that created the object
		return getLastUpdateFor(object, field, object.transaction, run).orElseThrow(() -> new DeserializationError("Did not find the last update for " + field + " of " + object));
	}

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
			requireBlockchainNotYetInitialized();
			return expandBlockchainWith(request, Transaction.mkFor(request, getNextTransaction(), this).getResponse());
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
			requireBlockchainNotYetInitialized();
			GameteCreationTransactionResponse response = Transaction.mkFor(request, getNextTransaction(), this).getResponse();
			expandBlockchainWith(request, response);
			return response.gamete;
		});
	}

	/**
	 * Expands this blockchain with a transaction that creates a red/green gamete, that is,
	 * a red/green externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the blockchain. It has
	 * no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionException if the transaction could not be completed successfully. In this case, the blockchain is not expanded
	 */
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			requireBlockchainNotYetInitialized();
			GameteCreationTransactionResponse response = Transaction.mkFor(request, getNextTransaction(), this).getResponse();
			expandBlockchainWith(request, response);
			return response.gamete;
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
			JarStoreTransactionResponse response = Transaction.mkFor(request, getNextTransaction(), this).getResponse();
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
	 *                              {@link io.takamaka.code.engine.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no constructor will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the constructor is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 */
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			ConstructorCallTransactionResponse response = Transaction.mkFor(request, getNextTransaction(), this).getResponse();
			expandBlockchainWith(request, response);

			if (response instanceof ConstructorCallTransactionFailedResponse)
				throw ((ConstructorCallTransactionFailedResponse) response).cause;
			else if (response instanceof ConstructorCallTransactionExceptionResponse) {
				ConstructorCallTransactionExceptionResponse ccter = (ConstructorCallTransactionExceptionResponse) response;
				throw new CodeExecutionException("constructor threw", ccter.classNameOfCause, ccter.messageOfCause, ccter.getStackTrace());
			}
			else
				return ((ConstructorCallTransactionSuccessfulResponse) response).newObject;
		});
	}

	/**
	 * Runs an instance method of an object in blockchain.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.takamaka.code.engine.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 */
	public final StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			MethodCallTransactionResponse response = runInstanceMethodCallTransaction(request, getNextTransaction());
			expandBlockchainWith(request, response);

			if (response instanceof MethodCallTransactionFailedResponse)
				throw ((MethodCallTransactionFailedResponse) response).cause;
			else if (response instanceof MethodCallTransactionExceptionResponse) {
				MethodCallTransactionExceptionResponse mcter = (MethodCallTransactionExceptionResponse) response;
				throw new CodeExecutionException("constructor threw", mcter.classNameOfCause, mcter.messageOfCause, mcter.getStackTrace());
			}
			else if (response instanceof VoidMethodCallTransactionSuccessfulResponse)
				return null;
			else
				return ((MethodCallTransactionSuccessfulResponse) response).result;
		});
	}

	/**
	 * Expands this blockchain with a transaction that runs a static method of a class in blockchain.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.takamaka.code.engine.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 */
	public final StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			MethodCallTransactionResponse response = runStaticMethodCallTransaction(request, getNextTransaction());
			expandBlockchainWith(request, response);

			if (response instanceof MethodCallTransactionFailedResponse)
				throw ((MethodCallTransactionFailedResponse) response).cause;
			else if (response instanceof MethodCallTransactionExceptionResponse) {
				MethodCallTransactionExceptionResponse mcter = (MethodCallTransactionExceptionResponse) response;
				throw new CodeExecutionException("constructor threw", mcter.classNameOfCause, mcter.messageOfCause, mcter.getStackTrace());
			}
			else if (response instanceof VoidMethodCallTransactionSuccessfulResponse)
				return null;
			else
				return ((MethodCallTransactionSuccessfulResponse) response).result;
		});
	}

	private void requireBlockchainNotYetInitialized() throws Exception {
		SequentialTransactionReference previous = getTopmostTransactionReference();
		if (previous != null && !(getRequestAt(previous) instanceof InitialTransactionRequest))
			throw new IllegalTransactionRequestException("This blockchain is already initialized");
	}

	/**
	 * Puts in the given set all the latest updates for the fields of eager type of the
	 * object at the given storage reference.
	 * 
	 * @param object the storage reference
	 * @param updates the set where the latest updates must be added
	 * @param eagerFields the number of eager fields whose latest update needs to be found
	 * @throws Exception if the operation fails
	 */
	private Stream<Update> collectEagerUpdatesFor(StorageReference object, Set<Update> updates, int eagerFields, TransactionRun run) throws Exception {
		// goes back from the transaction that precedes that being executed;
		// there is no reason to look before the transaction that created the object;
		// moreover, there is no reason to look beyond the total number of fields
		// whose update was expected to be found
		for (SequentialTransactionReference cursor = getTopmostTransactionReference(); updates.size() <= eagerFields && !cursor.isOlderThan(object.transaction); cursor = cursor.getPrevious())
			// adds the eager updates from the cursor, if any and if they are the latest
			addEagerUpdatesFor(object, cursor, updates, run);

		return updates.stream();
	}

	/**
	 * Adds, to the given set, the updates of eager fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the transaction
	 * @param updates the set where they must be added
	 * @throws IOException if there is an error while accessing the disk
	 */
	private void addEagerUpdatesFor(StorageReference object, TransactionReference transaction, Set<Update> updates, TransactionRun run) throws Exception {
		TransactionResponse response = getResponseAtAndCharge(transaction, run);
		if (response instanceof TransactionResponseWithUpdates)
			((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField && update.object.equals(object) && update.isEager() && !isAlreadyIn(update, updates))
				.forEach(updates::add);
	}

	/**
	 * Yields the response that generated the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @return the response
	 * @throws Exception if the response could not be found
	 */
	private TransactionResponse getResponseAtAndCharge(TransactionReference transaction, TransactionRun run) throws Exception {
		run.chargeForCPU(gasCostModel.cpuCostForGettingResponseAt(transaction));
		return run.getNode().getResponseAt(transaction);
	}

	/**
	 * Determines if the given set of updates contains an update for the
	 * same object and field as the given update.
	 * 
	 * @param update the given update
	 * @param updates the set
	 * @return true if and only if that condition holds
	 */
	private static boolean isAlreadyIn(Update update, Set<Update> updates) {
		return updates.stream().anyMatch(update::isForSamePropertyAs);
	}

	/**
	 * Yields the update to the given field of the object at the given reference,
	 * generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param transaction the block where the update is being looked for
	 * @return the update, if any. If the field of {@code reference} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdateFor(StorageReference object, FieldSignature field, TransactionReference transaction, TransactionRun run) throws Exception {
		TransactionResponse response = getResponseAtAndCharge(transaction, run);
		if (response instanceof TransactionResponseWithUpdates)
			return ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(object) && update.getField().equals(field))
				.findAny();
	
		return Optional.empty();
	}

	/**
	 * Determines if the given update affects a non-{@code final} eager field contained in the given set.
	 * 
	 * @param update the update
	 * @param eagerFields the set of all possible eager fields
	 * @return true if and only if that condition holds
	 */
	private boolean updatesNonFinalField(Update update, Set<Field> eagerFields, TransactionRun run) throws ClassNotFoundException {
		if (update instanceof UpdateOfField) {
			FieldSignature sig = ((UpdateOfField) update).getField();
			Class<?> type = run.getStorageTypeToClass().toClass(sig.type);
			String name = sig.name;
			return eagerFields.stream()
				.anyMatch(field -> !Modifier.isFinal(field.getModifiers()) && field.getType() == type && field.getName().equals(name));
		}

		return false;
	}

	/**
	 * Collects all eager fields of the given storage class, including those of its superclasses,
	 * up to and excluding {@link io.takamaka.code.lang.Storage}.
	 * 
	 * @param className the name of the storage class
	 * @return the eager fields
	 */
	private Set<Field> collectEagerFieldsOf(String className, TransactionRun run) throws ClassNotFoundException {
		EngineClassLoader classLoader = run.getClassLoader();
		Set<Field> bag = new HashSet<>();
		Class<?> storage = classLoader.getStorage();

		// fields added by instrumentation by Takamaka itself are not considered, since they are transient
		for (Class<?> clazz = classLoader.loadClass(className); clazz != storage; clazz = clazz.getSuperclass())
			Stream.of(clazz.getDeclaredFields())
			.filter(field -> !Modifier.isTransient(field.getModifiers())
					&& !Modifier.isStatic(field.getModifiers())
					&& classLoader.isEagerlyLoaded(field.getType()))
			.forEach(bag::add);

		return bag;
	}

	/**
	 * Calls the given callable. If if throws a {@link io.takamaka.code.engine.CodeExecutionException}, if throws it back
	 * unchanged. Otherwise, it wraps the exception into into a {@link io.hotmoka.beans.TransactionException}.
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
}