package io.takamaka.code.engine;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.nodes.AsynchronousNode;
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.OutOfGasError;
import io.hotmoka.nodes.SynchronousNode;

/**
 * A generic implementation of a node.
 * Specific implementations can subclass this class and implement the abstract template methods.
 */
public abstract class AbstractNode implements SynchronousNode, AsynchronousNode {

	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	@Override
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}

	/**
	 * Yields the history of the given object, that is,
	 * the references to the transactions that might have updated the
	 * given object, in reverse chronological order (from newest to oldest).
	 * This stream is an over-approximation, hence it might also contain transactions
	 * that did not affect the object, but it must include all that did it.
	 * If the node has some form of commit, this history must include also
	 * transactions executed but not yet committed.
	 * 
	 * @param object the object whose update history must be looked for
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest)
	 */
	protected abstract Stream<TransactionReference> getHistoryOf(StorageReference object);

	/**
	 * Determines if this node allows to execute initial transactions.
	 * 
	 * @return true if and only if this node doesn't allow any initial transaction anymore
	 */
	protected abstract boolean isInitialized();

	/**
	 * Takes note that this node doesn't allow any initial transaction anymore.
	 */
	protected abstract void markAsInitialized();

	@Override
	public final String getClassNameOf(StorageReference object) {
		try {
			TransactionResponse response = getResponseAt(object.transaction);
			if (response instanceof TransactionResponseWithUpdates) {
				Optional<ClassTag> classTag = ((TransactionResponseWithUpdates) response).getUpdates()
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst();

				if (classTag.isPresent())
					return classTag.get().className;
			}
		}
		catch (DeserializationError e) {
			throw e;
		}
		catch (Exception e) {
			throw new DeserializationError(e);
		}

		throw new DeserializationError("no class tag found for " + object);
	}

	@Override
	public final Stream<Update> getLastEagerUpdatesFor(StorageReference reference, Consumer<BigInteger> chargeForCPU, Function<String, Stream<Field>> eagerFields) throws Exception {
		TransactionReference transaction = reference.transaction;
	
		TransactionResponse response = getResponseAndCharge(transaction, chargeForCPU);
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
		Set<Field> allEagerFields = eagerFields.apply(classTag.get().className).collect(Collectors.toSet());
		Iterator<Update> it = updates.iterator();
		while (it.hasNext())
			if (updatesNonFinalField(it.next(), allEagerFields))
				it.remove();
	
		// the updates set contains the updates to eager final fields now:
		// we must still collect the latest updates to the eager non-final fields
		collectEagerUpdatesFor(reference, updates, allEagerFields.size(), chargeForCPU);

		return updates.stream();
	}

	@Override
	public final UpdateOfField getLastLazyUpdateToNonFinalFieldOf(StorageReference object, FieldSignature field, Consumer<BigInteger> chargeForCPU) throws Exception {
		for (TransactionReference transaction: getHistoryOf(object).collect(Collectors.toList())) {
			Optional<UpdateOfField> update = getLastUpdateFor(object, field, transaction, chargeForCPU);
			if (update.isPresent())
				return update.get();
		}

		throw new DeserializationError("Did not find the last update for " + field + " of " + object);
	}

	@Override
	public final UpdateOfField getLastLazyUpdateToFinalFieldOf(StorageReference object, FieldSignature field, Consumer<BigInteger> chargeForCPU) throws Exception {
		// accesses directly the transaction that created the object
		return getLastUpdateFor(object, field, object.transaction, chargeForCPU).orElseThrow(() -> new DeserializationError("Did not find the last update for " + field + " of " + object));
	}

	@Override
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			requireNodeUninitialized();
			return addJarStoreInitialTransactionInternal(request);
		});
	}

	/**
	 * Expands the store of this node with a transaction that
	 * installs a jar in it. It is guaranteed that this transaction can only occur during initialization
	 * of the node. It has no caller and requires no gas. The goal is to install, in the
	 * node, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. In this case, the node's store is not expanded
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļink io.hotmoka.beans.TransactionException}
	 */
	protected abstract TransactionReference addJarStoreInitialTransactionInternal(JarStoreInitialTransactionRequest request) throws Exception;

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			requireNodeUninitialized();
			return addGameteCreationTransactionInternal(request);
		});
	}

	/**
	 * Expands the store of this node with a transaction that creates a gamete, that is,
	 * an externally owned contract with the given initial amount of coins.
	 * It is guaranteed that this transaction can only occur during initialization of the node.
	 * It has no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionException if the transaction could not be completed successfully. In this case, the node's store is not expanded
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļink io.hotmoka.beans.TransactionException}
	 */
	protected abstract StorageReference addGameteCreationTransactionInternal(GameteCreationTransactionRequest request) throws Exception;

	@Override
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			requireNodeUninitialized();
			return addRedGreenGameteCreationTransactionInternal(request);
		});
	}

	/**
	 * Expands the store of this node with a transaction that creates a gamete, that is,
	 * an externally owned contract with the given initial amount of coins.
	 * It is guaranteed that this transaction can only occur during initialization of the node. It has
	 * no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionException if the transaction could not be completed successfully. In this case, the node's store is not expanded
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļink io.hotmoka.beans.TransactionException}
	 */
	protected abstract StorageReference addRedGreenGameteCreationTransactionInternal(RedGreenGameteCreationTransactionRequest request) throws Exception;

	@Override
	public final TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			TransactionReference transaction = addJarStoreTransactionInternal(request);
			markAsInitialized();
			return transaction;
		});
	}

	/**
	 * Expands the store of this node with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. If this occurs and the caller
	 *                              has been identified, the node store will still be expanded
	 *                              with a transaction that charges the gas limit to the caller, but no jar will be installed.
	 *                              Otherwise, the transaction will be rejected and not added to this node's store
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļink io.hotmoka.beans.TransactionException}
	 */
	protected abstract TransactionReference addJarStoreTransactionInternal(JarStoreTransactionRequest request) throws Exception;

	@Override
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			StorageReference newObject = addConstructorCallTransactionInternal(request);
			markAsInitialized();
			return newObject;
		});
	}

	/**
	 * Expands this node's store with a transaction that runs a constructor of a class.
	 * 
	 * @param request the request of the transaction
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws CodeExecutionException if the constructor is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                                {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                                If this occurs and the caller has been identified, the node's store will still be expanded
	 *                                with a transaction that charges all gas limit to the caller, but no constructor will be executed.
	 *                                Otherwise, the transaction will be rejected and not added to this node's store
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļink io.hotmoka.beans.TransactionException}
	 */
	protected abstract StorageReference addConstructorCallTransactionInternal(ConstructorCallTransactionRequest request) throws Exception;

	@Override
	public final StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			StorageValue result = addInstanceMethodCallTransactionInternal(request);
			markAsInitialized();
			return result;
		});
	}

	/**
	 * Expands this node's store with a transaction that runs an instance method of an object already in this node's store.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller has been identified, the node's store will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this node's store
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļink io.hotmoka.beans.TransactionException}
	 */
	protected abstract StorageValue addInstanceMethodCallTransactionInternal(InstanceMethodCallTransactionRequest request) throws Exception;

	@Override
	public final StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			StorageValue result = addStaticMethodCallTransactionInternal(request);
			markAsInitialized();
			return result;
		});
	}

	/**
	 * Expands this node's store with a transaction that runs a static method of a class in this node.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller has been identified, the node's store will still be expanded
	 *                              with a transaction that charges all gas limit to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this node's store
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļink io.hotmoka.beans.TransactionException}
	 */
	protected abstract StorageValue addStaticMethodCallTransactionInternal(StaticMethodCallTransactionRequest request) throws Exception;

	@Override
	public final StorageValue runViewInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> runViewInstanceMethodCallTransactionInternal(request));
	}

	/**
	 * Runs an instance {@code @@View} method of an object already in this node's store.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@linkplain io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. In all other cases, a {@linkplain io.hotmoka.beans.TransactionException} is thrown
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļinkplain io.hotmoka.beans.TransactionException}
	 */
	protected abstract StorageValue runViewInstanceMethodCallTransactionInternal(InstanceMethodCallTransactionRequest request) throws Exception;

	@Override
	public final StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> runViewStaticMethodCallTransactionInternal(request));
	}

	/**
	 * Runs a static {@code @@View} method of a class in this node.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s
	 * @throws CodeExecutionException if the method is annotated as {@linkplain io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. In all other cases, a {@linkplain io.hotmoka.beans.TransactionException} is thrown
	 * @throws Exception if the transaction could not be completed for an internal error; this will be wrapped into a {@ļinkplain io.hotmoka.beans.TransactionException}
	 */
	protected abstract StorageValue runViewStaticMethodCallTransactionInternal(StaticMethodCallTransactionRequest request) throws Exception;	

	@Override
	public final void postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException {
		wrapInCaseOfException(() -> postJarStoreTransactionInternal(request));
	}

	/**
	 * Posts a transaction that expands the store of this node with a transaction that installs a jar in it.
	 * If the transaction could not be completed successfully
	 * and the caller has been identified, the node store will still be expanded
	 * with a transaction that charges the gas limit to the caller, but no jar will be installed.
	 * Otherwise, the transaction will be rejected and not added to this node's store.
	 * 
	 * @param request the transaction request
	 * @throws Exception if an error prevented the transaction from being posted
	 */
	protected abstract void postJarStoreTransactionInternal(JarStoreTransactionRequest request) throws Exception;

	@Override
	public final CodeExecutionFuture<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> postConstructorCallTransactionInternal(request));
	}

	/**
	 * Posts a transaction that runs a constructor of a class in this node.
	 * If the transaction could not be completed successfully,
	 * for instance because of {@linkplain OutOfGasError}s and {@linkplain io.takamaka.code.lang.InsufficientFundsError}s,
	 * and the caller has been identified, the node's store will still be expanded
	 * with a transaction that charges all gas limit to the caller, but no constructor will be executed.
	 * Otherwise, the transaction will be rejected and not added to this node's store.
	 * If the constructor is annotated as {@linkplain io.takamaka.code.lang.ThrowsExceptions}
	 * and the constructor threw a {@linkplain CodeExecutionException} then,
	 * from the point of view of Takamaka, the transaction was successful, it gets added to this node's store
	 * and the consumed gas gets charged to the caller.
	 * 
	 * @param request the request of the transaction
	 * @return the future holding the result of the computation
	 * @throws Exception if an error prevented the transaction from being posted
	 */
	protected abstract CodeExecutionFuture<StorageReference> postConstructorCallTransactionInternal(ConstructorCallTransactionRequest request) throws Exception;

	@Override
	public final CodeExecutionFuture<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> postInstanceMethodCallTransactionInternal(request));
	}

	/**
	 * Posts a transaction that runs an instance method of an object already in this node's store.
	 * If the transaction could not be completed successfully, also because of
	 * {@linkplain OutOfGasError}s and {@linkplain io.takamaka.code.lang.InsufficientFundsError}s,
	 * and the caller has been identified, the node's store will still be expanded
	 * with a transaction that charges all gas limit to the caller, but no method will be executed.
	 * Otherwise, the transaction will be rejected and not added to this node's store.
	 * If the method is annotated as {@linkplain io.takamaka.code.lang.ThrowsExceptions} and its execution
	 * failed with a checked exception then, from the point of view of Takamaka,
	 * the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 * 
	 * @param request the transaction request
	 * @return the future holding the result of the transaction
	 * @throws Exception if an error prevented the transaction from being posted
	 */
	protected abstract CodeExecutionFuture<StorageValue> postInstanceMethodCallTransactionInternal(InstanceMethodCallTransactionRequest request) throws Exception;

	@Override
	public final void postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException {
		wrapInCaseOfException(() -> postStaticMethodCallTransactionInternal(request));
	}

	/**
	 * Posts a request that runs a static method of a class in this node.
	 * If the transaction could not be completed successfully, also because of
	 * {@linkplain OutOfGasError}s and {@linkplain io.takamaka.code.lang.InsufficientFundsError}s,
	 * and the caller has been identified, the node's store will still be expanded
	 * with a transaction that charges all gas limit to the caller, but no method will be executed.
	 * Otherwise, the transaction will be rejected and not added to this node's store.
	 * If the method is annotated as {@linkplain io.takamaka.code.lang.ThrowsExceptions} and its execution
	 * failed with a checked exception then, from the point of view of Takamaka,
	 * the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 * 
	 * @param request the transaction request
	 * @throws Exception if an error prevented the transaction from being posted
	 */
	protected abstract void postStaticMethodCallTransactionInternal(StaticMethodCallTransactionRequest request) throws Exception;

	/**
	 * Checks if this node is still not fully initialized, so that further initial transactions can still
	 * be executed. As soon as a non-initial transaction is run with this node, it is considered as initialized.
	 * 
	 * @throws IllegalStateException if this node is already initialized
	 */
	private void requireNodeUninitialized() throws IllegalStateException {
		if (isInitialized())
			throw new IllegalStateException("this node is already initialized");
	}

	/**
	 * Adds, to the given set, all the latest updates for the fields of eager type of the
	 * object at the given storage reference.
	 * 
	 * @param object the storage reference
	 * @param updates the set where the latest updates must be added
	 * @param eagerFields the number of eager fields whose latest update needs to be found
	 * @param chargeForCPU the code to run to charge gas for CPU execution
	 * @throws Exception if the operation fails
	 */
	private void collectEagerUpdatesFor(StorageReference object, Set<Update> updates, int eagerFields, Consumer<BigInteger> chargeForCPU) throws Exception {
		// scans the history of the object; there is no reason to look beyond the total number of fields whose update was expected to be found
		for (TransactionReference transaction: getHistoryOf(object).collect(Collectors.toList()))
			if (updates.size() <= eagerFields)
				addEagerUpdatesFor(object, transaction, updates, chargeForCPU);
			else
				return;
	}

	/**
	 * Adds, to the given set, the updates of eager fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param updates the set where they must be added
	 * @param chargeForCPU the code to run to charge gas for CPU execution
	 * @throws Exception if there is an error accessing the updates
	 */
	private void addEagerUpdatesFor(StorageReference object, TransactionReference transaction, Set<Update> updates, Consumer<BigInteger> chargeForCPU) throws Exception {
		TransactionResponse response = getResponseAndCharge(transaction, chargeForCPU);
		if (response instanceof TransactionResponseWithUpdates)
			((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField && update.object.equals(object) && update.isEager() && !isAlreadyIn(update, updates))
				.forEach(updates::add);
	}

	/**
	 * Yields the response that generated the given transaction and charges for that operation.
	 * 
	 * @param transaction the reference to the transaction
	 * @param chargeForCPU the code to run to charge gas for CPU execution
	 * @return the response
	 * @throws Exception if the response could not be found
	 */
	private TransactionResponse getResponseAndCharge(TransactionReference transaction, Consumer<BigInteger> chargeForCPU) throws Exception {
		chargeForCPU.accept(getGasCostModel().cpuCostForGettingResponseAt(transaction));
		return getResponseAt(transaction);
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
	 * @param transaction the reference to the transaction
	 * @param chargeForCPU the code to run to charge gas for CPU execution
	 * @return the update, if any. If the field of {@code object} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdateFor(StorageReference object, FieldSignature field, TransactionReference transaction, Consumer<BigInteger> chargeForCPU) throws Exception {
		TransactionResponse response = getResponseAndCharge(transaction, chargeForCPU);

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
	private static boolean updatesNonFinalField(Update update, Set<Field> eagerFields) throws ClassNotFoundException {
		if (update instanceof UpdateOfField) {
			FieldSignature sig = ((UpdateOfField) update).getField();
			StorageType type = sig.type;
			String name = sig.name;
			return eagerFields.stream()
				.anyMatch(field -> !Modifier.isFinal(field.getModifiers()) && hasType(field, type) && field.getName().equals(name));
		}

		return false;
	}

	/**
	 * Determines if the given field has the given storage type.
	 * 
	 * @param field the field
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	private static boolean hasType(Field field, StorageType type) {
		Class<?> fieldType = field.getType();
		if (type instanceof BasicTypes)
			switch ((BasicTypes) type) {
			case BOOLEAN: return fieldType == boolean.class;
			case BYTE: return fieldType == byte.class;
			case CHAR: return fieldType == char.class;
			case SHORT: return fieldType == short.class;
			case INT: return fieldType == int.class;
			case LONG: return fieldType == long.class;
			case FLOAT: return fieldType == float.class;
			case DOUBLE: return fieldType == double.class;
			default: throw new IllegalStateException("unexpected basic type " + type);
			}
		else if (type instanceof ClassType)
			return ((ClassType) type).name.equals(fieldType.getName());
		else
			throw new IllegalStateException("unexpected storage type " + type);
	}

	/**
	 * Calls the given callable. If if throws an exception, it wraps it into an {@link io.hotmoka.beans.TransactionException}.
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
			throw wrapAsTransactionException(t);
		}
	}

	/**
	 * Calls the given callable. If if throws a {@link io.hotmoka.beans.CodeExecutionException}, if throws it back
	 * unchanged. Otherwise, it wraps the exception into an {@link io.hotmoka.beans.TransactionException}.
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
			throw wrapAsTransactionException(t);
		}
	}

	private interface Task {
		void run() throws Exception;
	}

	/**
	 * Runs the given runnable. If if throws a {@link io.hotmoka.beans.CodeExecutionException}, if throws it back
	 * unchanged. Otherwise, it wraps the exception into an {@link io.hotmoka.beans.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws CodeExecutionException the unwrapped exception
	 * @throws TransactionException the wrapped exception
	 */
	private static void wrapInCaseOfException(Task what) throws TransactionException {
		try {
			what.run();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t);
		}
	}

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @return the wrapped or original exception
	 */
	private static TransactionException wrapAsTransactionException(Throwable t) {
		return t instanceof TransactionException ? (TransactionException) t : new TransactionException(t);
	}
}