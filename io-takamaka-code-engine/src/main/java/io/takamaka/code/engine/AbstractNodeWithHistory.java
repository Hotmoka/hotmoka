package io.takamaka.code.engine;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.nodes.NodeWithHistory;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.verification.IncompleteClasspathError;

/**
 * A generic implementation of a node with history.
 * Specific implementations can subclass this and implement the abstract template methods.
 */
public abstract class AbstractNodeWithHistory<C extends Config> extends AbstractNode<C> implements NodeWithHistory {
	private final static Logger logger = LoggerFactory.getLogger(AbstractNodeWithHistory.class);

	/**
	 * The cache for the {@linkplain #getRequestAt(TransactionReference)} method.
	 */
	private final LRUCache<TransactionReference, TransactionRequest<?>> getRequestAtCache;

	/**
	 * The cache for the {@linkplain #getResponseAt(TransactionReference)} method.
	 */
	private final LRUCache<TransactionReference, TransactionResponse> getResponseAtCache;

	/**
	 * Builds the node.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractNodeWithHistory(C config) {
		super(config);

		try {
			this.getRequestAtCache = new LRUCache<>(config.requestCacheSize);
			this.getResponseAtCache = new LRUCache<>(config.responseCacheSize);
		}
		catch (Exception e) {
			logger.error("failed to create the node", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public final TransactionRequest<?> getRequestAt(TransactionReference reference) throws NoSuchElementException {
		try {
			if (!isCommitted(reference))
				throw new NoSuchElementException("unknown transaction reference " + reference);
	
			return getRequestAtCache.computeIfAbsent(reference, this::getRequest);
		}
		catch (NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public final TransactionResponse getResponseAt(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		try {
			if (!isCommitted(reference))
				throw new NoSuchElementException("unknown transaction reference " + reference);
	
			return getResponseAtCache.computeIfAbsent(reference, this::getResponse);
		}
		catch (TransactionRejectedException | NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public final ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
		try {
			// we go straight to the transaction that created the object
			TransactionResponse response;
			try {
				response = getResponseAt(reference.transaction);
			}
			catch (TransactionRejectedException e) {
				throw new NoSuchElementException("unknown transaction reference " + reference.transaction);
			}
	
			if (!(response instanceof TransactionResponseWithUpdates))
				throw new NoSuchElementException("transaction reference " + reference.transaction + " does not contain updates");
	
			return ((TransactionResponseWithUpdates) response).getUpdates()
					.filter(update -> update instanceof ClassTag && update.object.equals(reference))
					.map(update -> (ClassTag) update)
					.findFirst().get();
		}
		catch (NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public final Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
		try {
			ClassTag classTag = getClassTag(reference);
			EngineClassLoader classLoader = new EngineClassLoader(classTag.jar, this);
			return getLastEagerOrLazyUpdates(reference, classLoader);
		}
		catch (NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	protected final TransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarAtUncommitted(TransactionReference reference) throws NoSuchElementException {
		TransactionResponse response = getResponseUncommittedAt(reference);
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new NoSuchElementException("the transaction " + reference + " did not install a jar in store");
	
		return (TransactionResponseWithInstrumentedJar) response;
	}

	@Override
	protected final Stream<Update> getLastEagerUpdatesUncommitted(StorageReference object, EngineClassLoader classLoader, Consumer<BigInteger> chargeGasForCPU) {
		TransactionReference transaction = object.transaction;
		chargeGasForCPU.accept(getGasCostModel().cpuCostForGettingResponseAt(transaction));
		TransactionResponse response = getResponseUncommittedAt(transaction);
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new DeserializationError("Storage reference " + object + " does not contain updates");
	
		Set<Update> updates = ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update.object.equals(object))
				.filter((Predicate<Update>) Update::isEager)
				.collect(Collectors.toSet());
	
		Optional<ClassTag> classTag = updates.stream()
				.filter(update -> update instanceof ClassTag)
				.map(update -> (ClassTag) update)
				.findAny();
	
		if (!classTag.isPresent())
			throw new DeserializationError("No class tag found for " + object);
	
		// we drop updates to non-final fields
		Set<Field> allFields = collectAllFieldsOf(classTag.get().className, classLoader, true);
		Iterator<Update> it = updates.iterator();
		while (it.hasNext())
			if (updatesNonFinalField(it.next(), allFields))
				it.remove();
	
		// the updates set contains the updates to final fields now:
		// we must still collect the latest updates to non-final fields
		Stream<TransactionReference> history = getHistoryUncommitted(object);
		collectUpdatesForUncommitted(object, history, updates, allFields.size(), chargeGasForCPU);
	
		return updates.stream();
	}

	@Override
	protected final UpdateOfField getLastLazyUpdateToNonFinalFieldUncommited(StorageReference storageReference, FieldSignature field, Consumer<BigInteger> chargeForCPU) {
		for (TransactionReference transaction: getHistoryUncommitted(storageReference).collect(Collectors.toList())) { //TODO
			Optional<UpdateOfField> update = getLastUpdateForUncommitted(storageReference, field, transaction, chargeForCPU);
			if (update.isPresent())
				return update.get();
		}
	
		throw new DeserializationError("did not find the last update for " + field + " of " + storageReference);
	}

	@Override
	protected final UpdateOfField getLastLazyUpdateToFinalFieldUncommitted(StorageReference object, FieldSignature field, Consumer<BigInteger> chargeForCPU) {
		// accesses directly the transaction that created the object
		return getLastUpdateForUncommitted(object, field, object.transaction, chargeForCPU).orElseThrow(() -> new DeserializationError("Did not find the last update for " + field + " of " + object));
	}

	@Override
	protected final TransactionResponse pollResponseComputedFor(TransactionReference reference) throws TransactionRejectedException {
		return getResponseAt(reference);
	}

	/**
	 * Yields the history of the given object, that is,
	 * the references to the transactions that provide information about
	 * its current state, in reverse chronological order (from newest to oldest).
	 * If the node has some form of commit, this history includes only
	 * already committed transactions.
	 * 
	 * @param object the object whose update history must be looked for
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest). If {@code object} has currently no history, it yields an
	 *         empty stream, but never throws an exception
	 */
	protected abstract Stream<TransactionReference> getHistory(StorageReference object);

	/**
	 * Yields the history of the given object, that is,
	 * the references to the transactions that provide information about
	 * its current state, in reverse chronological order (from newest to oldest).
	 * If the node has some form of commit, this history include also
	 * uncommitted transactions.
	 * 
	 * @param object the object whose update history must be looked for
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest). If {@code object} has currently no history, it yields an
	 *         empty stream, but never throws an exception
	 */
	protected abstract Stream<TransactionReference> getHistoryUncommitted(StorageReference object);

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method is called only when
	 * the transaction has been already committed.
	 * The successful result of this method is wrapped into a cache and used to implement
	 * {@linkplain #getRequestAt(TransactionReference)}.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 */
	protected abstract TransactionRequest<?> getRequest(TransactionReference reference);

	/**
	 * Yields the response generated for the request for the given transaction.
	 * If this node has some form of commit, then this method is called only when
	 * the transaction has been already committed.
	 * The successful result of this method is wrapped into a cache and used to implement
	 * {@linkplain #getResponseAt(TransactionReference)}.
	 * 
	 * @param reference the reference to the transaction
	 * @return the response
	 * @throws TransactionRejectedException if there is a request for that transaction but it failed with this exception
	 */
	protected abstract TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException;

	/**
	 * Yields the response generated for the request for the given transaction.
	 * It is guaranteed that the transaction has been already successfully delivered,
	 * hence a response must exist in store.
	 * The successful result of this method is wrapped into a cache and used to implement
	 * {@linkplain #getResponseUncommittedAt(TransactionReference)}.
	 * 
	 * @param reference the reference to the transaction
	 * @return the response
	 */
	protected abstract TransactionResponse getResponseUncommitted(TransactionReference reference);

	/**
	 * A cached version of {@linkplain #getResponseUncommitted(TransactionReference)}.
	 * 
	 * @param reference the reference of the transaction, possibly not yet committed
	 * @return the response of the transaction
	 */
	private final TransactionResponse getResponseUncommittedAt(TransactionReference reference) {
		try {
			return getResponseAtCache.computeIfAbsent(reference, this::getResponseUncommitted);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the last updates to the fields of the given object.
	 * 
	 * @param object the reference to the object
	 * @param classLoader the class loader
	 * @return the updates
	 */
	private Stream<Update> getLastEagerOrLazyUpdates(StorageReference object, EngineClassLoader classLoader) {
		TransactionReference transaction = object.transaction;
		TransactionResponse response = getResponseUncommittedAt(transaction); // we actually checked that it is committed
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new DeserializationError("Storage reference " + object + " does not contain updates");
	
		Set<Update> updates = ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update.object.equals(object))
				.collect(Collectors.toSet());
	
		Optional<ClassTag> classTag = updates.stream()
				.filter(update -> update instanceof ClassTag)
				.map(update -> (ClassTag) update)
				.findAny();
	
		if (!classTag.isPresent())
			throw new DeserializationError("No class tag found for " + object);
	
		// we drop updates to non-final fields
		Set<Field> allFields = collectAllFieldsOf(classTag.get().className, classLoader, true);
		Iterator<Update> it = updates.iterator();
		while (it.hasNext())
			if (updatesNonFinalField(it.next(), allFields))
				it.remove();
	
		// the updates set contains the updates to final fields now:
		// we must still collect the latest updates to non-final fields
		collectUpdatesFor(object, getHistory(object), updates, allFields.size());
	
		return updates.stream();
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
	private Optional<UpdateOfField> getLastUpdateForUncommitted(StorageReference object, FieldSignature field, TransactionReference transaction, Consumer<BigInteger> chargeForCPU) {
		chargeForCPU.accept(getGasCostModel().cpuCostForGettingResponseAt(transaction));

		TransactionResponse response = getResponseUncommittedAt(transaction);

		if (response instanceof TransactionResponseWithUpdates)
			return ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(object) && update.getField().equals(field))
				.findFirst();
	
		return Optional.empty();
	}

	/**
	 * Determines if the given update affects a non-{@code final} field contained in the given set.
	 * 
	 * @param update the update
	 * @param fields the set of all possible fields
	 * @return true if and only if that condition holds
	 */
	private static boolean updatesNonFinalField(Update update, Set<Field> fields) {
		if (update instanceof UpdateOfField) {
			FieldSignature sig = ((UpdateOfField) update).getField();
			StorageType type = sig.type;
			String name = sig.name;
			return fields.stream()
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
	 * Adds, to the given set, all the latest updates to the fields of the
	 * object at the given storage reference.
	 * 
	 * @param object the storage reference
	 * @param updates the set where the latest updates must be added
	 * @param fields the number of fields whose latest update needs to be found
	 */
	private void collectUpdatesFor(StorageReference object, Stream<TransactionReference> history, Set<Update> updates, int fields) {
		// scans the history of the object; there is no reason to look beyond the total number of fields whose update was expected to be found
		history.forEachOrdered(transaction -> {
			if (updates.size() <= fields)
				addUpdatesFor(object, transaction, updates);
		});
	}

	/**
	 * Adds, to the given set, all the latest updates to the fields of the
	 * object at the given storage reference.
	 * 
	 * @param object the storage reference
	 * @param updates the set where the latest updates must be added
	 * @param fields the number of fields whose latest update needs to be found
	 * @param chargeGasForCPU what to apply to charge gas for CPU usage
	 */
	private void collectUpdatesForUncommitted(StorageReference object, Stream<TransactionReference> history, Set<Update> updates, int fields, Consumer<BigInteger> chargeGasForCPU) {
		// scans the history of the object; there is no reason to look beyond the total number of fields whose update was expected to be found
		history.forEachOrdered(transaction -> {
			if (updates.size() <= fields)
				addUpdatesForUncommitted(object, transaction, chargeGasForCPU, updates);
		});
	}

	/**
	 * Adds, to the given set, the updates of the eager fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param chargeGasForCPU what to apply to charge gas for CPU usage
	 * @param updates the set where they must be added
	 */
	private void addUpdatesForUncommitted(StorageReference object, TransactionReference transaction, Consumer<BigInteger> chargeGasForCPU, Set<Update> updates) {
		chargeGasForCPU.accept(getGasCostModel().cpuCostForGettingResponseAt(transaction));
		TransactionResponse response = getResponseUncommittedAt(transaction);
		if (response instanceof TransactionResponseWithUpdates)
			((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField && update.object.equals(object) && update.isEager() && !isAlreadyIn(update, updates))
				.forEach(updates::add);
	}

	/**
	 * Adds, to the given set, the updates of the fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param updates the set where they must be added
	 */
	private void addUpdatesFor(StorageReference object, TransactionReference transaction, Set<Update> updates) {
		try {
			TransactionResponse response = getResponseAt(transaction);
			if (response instanceof TransactionResponseWithUpdates)
				((TransactionResponseWithUpdates) response).getUpdates()
					.filter(update -> update instanceof UpdateOfField && update.object.equals(object) && !isAlreadyIn(update, updates))
					.forEach(updates::add);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
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
	 * Collects the instance fields in the given class or in its superclasses.
	 * 
	 * @param className the name of the class
	 * @param classLoader the class loader that can be used to inspect {@code className}
	 * @param onlyEager true if and only if only the eager fields must be collected
	 * @return the fields
	 */
	private static Set<Field> collectAllFieldsOf(String className, EngineClassLoader classLoader, boolean onlyEager) {
		Set<Field> bag = new HashSet<>();
		Class<?> storage = classLoader.getStorage();

		try {
			// fields added in class storage by instrumentation by Takamaka itself are not considered, since they are transient
			for (Class<?> clazz = classLoader.loadClass(className); clazz != storage; clazz = clazz.getSuperclass())
				Stream.of(clazz.getDeclaredFields())
					.filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
					.filter(field -> !onlyEager || classLoader.isEagerlyLoaded(field.getType()))
					.forEach(bag::add);
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}

		return bag;
	}
}