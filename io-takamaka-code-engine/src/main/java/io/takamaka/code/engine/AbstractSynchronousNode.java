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
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
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
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.nodes.SynchronousNode;

/**
 * A generic implementation of a synchronous node, that executes transactions, adds
 * updates to the store of the node and yields the response of the transactions.
 * For a synchronous node, transactions have a time ordering,
 * so that it is possible to know which has been added before in the node.
 * Specific implementations can subclass this class and implement the abstract template methods.
 */
public abstract class AbstractSynchronousNode extends AbstractNode implements SynchronousNode {

	/**
	 * True if at least a non-initial transaction has been already executed on this node.
	 */
	private boolean initialized;

	/**
	 * Yields the reference to topmost transaction after which any new transaction will be executed.
	 * 
	 * @return the reference to the topmost transaction, if any. Yields {@code null} if
	 *         the node has executed no transactions up to now
	 */
	protected abstract SequentialTransactionReference getTopmostTransactionReference();

	/**
	 * Yields the reference that must be used to refer to a new transaction
	 * that follows the topmost one.
	 * 
	 * @return the reference to the next transaction
	 */
	protected abstract SequentialTransactionReference getNextTransaction();

	/**
	 * Expands the store of this node with a transaction, that is added after the topmost one and
	 * becomes the new topmost transaction.
	 * 
	 * @param <Request> the type of the request of the transaction
	 * @param <Response> the type of the response of the transaction
	 * @param transaction the transaction
	 * @throws Exception if the expansion cannot be completed
	 */
	protected abstract <Request extends TransactionRequest<Response>, Response extends TransactionResponse> void expandStoreWith(Transaction<Request, Response> transaction) throws Exception;

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
		// goes back from the previous transaction; there is no reason to look before the transaction that created the object
		for (SequentialTransactionReference cursor = getTopmostTransactionReference(); !cursor.isOlderThan(object.transaction); cursor = cursor.getPrevious()) {
			Optional<UpdateOfField> update = getLastUpdateFor(object, field, cursor, chargeForCPU);
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
			TransactionReference transactionReference = getNextTransaction();
			Transaction<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> transaction = Transaction.mkFor(request, transactionReference, this);
			expandStoreWith(transaction);
			return transaction.getResponse().getOutcomeAt(transactionReference);
		});
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			requireNodeUninitialized();
			Transaction<GameteCreationTransactionRequest, GameteCreationTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
			expandStoreWith(transaction);
			return transaction.getResponse().getOutcome();
		});
	}

	@Override
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			requireNodeUninitialized();
			Transaction<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
			expandStoreWith(transaction);
			return transaction.getResponse().getOutcome();
		});
	}

	@Override
	public final TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException {
		return wrapInCaseOfException(() -> {
			TransactionReference transactionReference = getNextTransaction();
			Transaction<JarStoreTransactionRequest, JarStoreTransactionResponse> transaction = Transaction.mkFor(request, transactionReference, this);
			expandStoreWith(transaction);
			initialized = true;
			return transaction.getResponse().getOutcomeAt(transactionReference);
		});
	}

	@Override
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			Transaction<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
			expandStoreWith(transaction);
			initialized = true;
			return transaction.getResponse().getOutcome();
		});
	}

	@Override
	public final StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			Transaction<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
			expandStoreWith(transaction);
			initialized = true;
			return transaction.getResponse().getOutcome();
		});
	}

	@Override
	public final StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> {
			Transaction<StaticMethodCallTransactionRequest, MethodCallTransactionResponse> transaction = Transaction.mkFor(request, getNextTransaction(), this);
			expandStoreWith(transaction);
			initialized = true;
			return transaction.getResponse().getOutcome();
		});
	}

	@Override
	public final StorageValue runViewInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> Transaction.mkForView(request, getNextTransaction(), this).getResponse().getOutcome());
	}

	@Override
	public final StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException {
		return wrapWithCodeInCaseOfException(() -> Transaction.mkForView(request, getNextTransaction(), this).getResponse().getOutcome());
	}

	/**
	 * Checks if this node is still not fully initialized, so that further initial transactions can still
	 * be executed. As soon as a non-initial transaction is run with this node, it is considered as initialized.
	 * 
	 * @throws IllegalStateException if this node is already initialized
	 */
	private void requireNodeUninitialized() throws IllegalStateException {
		if (initialized)
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
		// goes back from the transaction that precedes that being executed;
		// there is no reason to look before the transaction that created the object;
		// moreover, there is no reason to look beyond the total number of fields
		// whose update was expected to be found
		for (SequentialTransactionReference cursor = getTopmostTransactionReference(); updates.size() <= eagerFields && !cursor.isOlderThan(object.transaction); cursor = cursor.getPrevious())
			// adds the eager updates from the cursor, if any and if they are the latest
			addEagerUpdatesFor(object, cursor, updates, chargeForCPU);
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