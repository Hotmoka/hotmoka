package io.takamaka.code.engine;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
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
import io.hotmoka.beans.values.StorageValue;
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
	 * A cache for {@linkplain #getHistory(StorageReference)}.
	 */
	private final LRUCache<StorageReference, TransactionReference[]> historyCache;

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
			this.historyCache = new LRUCache<>(config.historyCacheSize);

			if (config.delete) {
				deleteRecursively(config.dir);  // cleans the directory where the node's data live
				Files.createDirectories(config.dir);
			}

			addShutdownHook();
		}
		catch (Exception e) {
			logger.error("failed to create the node", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the history of the given object, that is,
	 * the references to the transactions that provide information about
	 * its current state, in reverse chronological order (from newest to oldest).
	 * If the node has some form of commit, this history must include also
	 * transactions executed but not yet committed.
	 * 
	 * @param object the object whose update history must be looked for
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest). If {@code object} has currently no history, it yields an
	 *         empty stream, but never throw an exception
	 */
	protected abstract Stream<TransactionReference> getHistory(StorageReference object);

	/**
	 * Determines if the transaction with the given reference has been committed.
	 * If this mode has no form of commit, then answer true, always.
	 * 
	 * @param reference the reference
	 * @return true if and only if {@code reference} has been committed already
	 */
	protected abstract boolean isCommitted(TransactionReference reference);

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

	@Override
	protected final TransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarUncommittedAt(TransactionReference reference) throws IllegalArgumentException {
		TransactionResponse response = getResponseUncommittedAt(reference);
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new IllegalArgumentException("the transaction " + reference + " did not install a jar in store");

		return (TransactionResponseWithInstrumentedJar) response;
	}

	@Override
	protected final Stream<Update> getLastUpdates(StorageReference object, boolean onlyEager, EngineClassLoader classLoader, Consumer<BigInteger> chargeGasForCPU) {
		TransactionReference transaction = object.transaction;
	
		chargeGasForCPU.accept(getGasCostModel().cpuCostForGettingResponseAt(transaction));
		TransactionResponse response = getResponseUncommittedAt(transaction);
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new DeserializationError("Storage reference " + object + " does not contain updates");
	
		Predicate<Update> selector = onlyEager ? Update::isEager : (__ -> true);
	
		Set<Update> updates = ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update.object.equals(object))
				.filter(selector)
				.collect(Collectors.toSet());
	
		Optional<ClassTag> classTag = updates.stream()
				.filter(update -> update instanceof ClassTag)
				.map(update -> (ClassTag) update)
				.findAny();
	
		if (!classTag.isPresent())
			throw new DeserializationError("No class tag found for " + object);
	
		// we drop updates to non-final fields
		Set<Field> allFields = collectAllFieldsOf(classTag.get().className, classLoader, onlyEager);
		Iterator<Update> it = updates.iterator();
		while (it.hasNext())
			if (updatesNonFinalField(it.next(), allFields))
				it.remove();
	
		// the updates set contains the updates to final fields now:
		// we must still collect the latest updates to non-final fields
		collectUpdatesFor(object, updates, allFields.size(), selector, chargeGasForCPU);
	
		return updates.stream();
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
			return getLastUpdates(reference, false, classLoader, __ -> {});
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
	public final UpdateOfField getLastLazyUpdateToNonFinalField(StorageReference storageReference, FieldSignature field, Consumer<BigInteger> chargeForCPU) {
		for (TransactionReference transaction: getHistoryWithCache(storageReference, this::getHistory).collect(Collectors.toList())) {
			Optional<UpdateOfField> update = getLastUpdateFor(storageReference, field, transaction, chargeForCPU);
			if (update.isPresent())
				return update.get();
		}

		throw new DeserializationError("did not find the last update for " + field + " of " + storageReference);
	}

	@Override
	public final UpdateOfField getLastLazyUpdateToFinalField(StorageReference object, FieldSignature field, Consumer<BigInteger> chargeForCPU) {
		// accesses directly the transaction that created the object
		return getLastUpdateFor(object, field, object.transaction, chargeForCPU).orElseThrow(() -> new DeserializationError("Did not find the last update for " + field + " of " + object));
	}

	@Override
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return ((JarStoreInitialTransactionResponse) waitForResponse(reference)).getOutcomeAt(reference);
		});
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		wrapInCaseOfExceptionSimple(() -> waitForResponse(postRequest(request))); // result unused
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> ((GameteCreationTransactionResponse) waitForResponse(postRequest(request))).getOutcome());
	}

	@Override
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> ((GameteCreationTransactionResponse) waitForResponse(postRequest(request))).getOutcome());
	}

	@Override
	public final TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
		return wrapInCaseOfExceptionMedium(() -> postJarStoreTransaction(request).get());
	}

	@Override
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> postConstructorCallTransaction(request).get());
	}

	@Override
	public final StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> postInstanceMethodCallTransaction(request).get());
	}

	@Override
	public final StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> postStaticMethodCallTransaction(request).get());
	}

	@Override
	public final StorageValue runViewInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> {
			TransactionReference reference = referenceOf(request);
			logger.info(reference + ": running start (" + request.getClass().getSimpleName() + ')');
			StorageValue result = ResponseBuilder.ofView(reference, request, this).build().getOutcome();
			logger.info(reference + ": running success");
			return result;
		});
	}

	@Override
	public final StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> {
			TransactionReference reference = referenceOf(request);
			logger.info(reference + ": running start (" + request.getClass().getSimpleName() + ')');
			StorageValue result = ResponseBuilder.ofView(reference, request, this).build().getOutcome();
			logger.info(reference + ": running success");
			return result;
		});
	}

	@Override
	public final JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return jarSupplierFor(() -> ((JarStoreTransactionResponse) waitForResponse(reference)).getOutcomeAt(reference));
		});
	}

	@Override
	public final CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return codeSupplierFor(() -> ((ConstructorCallTransactionResponse) waitForResponse(reference)).getOutcome());
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return codeSupplierFor(() -> ((MethodCallTransactionResponse) waitForResponse(reference)).getOutcome());
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = postRequest(request);
			return codeSupplierFor(() -> ((MethodCallTransactionResponse) waitForResponse(reference)).getOutcome());
		});
	}

	/**
	 * A cached version of {@linkplain #getHistory(StorageReference)}.
	 * 
	 * @param object the object whose history must be looked for
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest)
	 */
	public final Stream<TransactionReference> getHistoryWithCache(StorageReference object) {
		return getHistoryWithCache(object, this::getHistory);
	}

	/**
	 * Deletes the given directory, recursively.
	 * 
	 * @param dir the directory to delete
	 * @throws IOException if the directory or some of its subdirectories cannot be deleted
	 */
	private static void deleteRecursively(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

	/**
	 * Adds a shutdown hook that shuts down the blockchain orderly if the JVM terminates.
	 */
	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				close();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}

	final TransactionResponse getResponseUncommittedAt(TransactionReference reference) {
		try {
			return getResponseAtCache.computeIfAbsent(reference, this::getResponseUncommitted);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * A cached version of {@linkplain #getHistory(StorageReference)}.
	 * 
	 * @param object the object whose history must be looked for
	 * @param getHistory the function to call in case of cache miss
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest)
	 */
	final Stream<TransactionReference> getHistoryWithCache(StorageReference object, Function<StorageReference, Stream<TransactionReference>> getHistory) {
		TransactionReference[] result = historyCache.computeIfAbsentNoException(object, reference -> getHistory.apply(reference).toArray(TransactionReference[]::new));
		return result != null ? Stream.of(result) : Stream.empty();
	}

	/**
	 * A cached version of {@linkplain #setHistory(StorageReference, Stream).
	 * 
	 * @param object the object whose history is set
	 * @param history the stream that will become the history of the object,
	 *                replacing its previous history
	 */
	final void setHistoryWithCache(StorageReference object, List<TransactionReference> history, BiConsumer<StorageReference, Stream<TransactionReference>> setHistory) {
		TransactionReference[] historyAsArray = history.toArray(new TransactionReference[history.size()]);
		setHistory.accept(object, history.stream());
		historyCache.put(object, historyAsArray);
	}

	@Override
	protected final TransactionResponse pollResponseComputedFor(TransactionReference reference) throws TransactionRejectedException {
		return getResponseAt(reference);
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
	private Optional<UpdateOfField> getLastUpdateFor(StorageReference object, FieldSignature field, TransactionReference transaction, Consumer<BigInteger> chargeForCPU) {
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
	 * @param selector a selector on the fields that are being considered
	 * @param chargeGasForCPU what to apply to charge gas for CPU usage
	 */
	private void collectUpdatesFor(StorageReference object, Set<Update> updates, int fields, Predicate<Update> selector, Consumer<BigInteger> chargeGasForCPU) {
		// scans the history of the object; there is no reason to look beyond the total number of fields whose update was expected to be found
		getHistoryWithCache(object).forEachOrdered(transaction -> {
			if (updates.size() <= fields)
				addUpdatesFor(object, transaction, selector, chargeGasForCPU, updates);
		});
	}

	/**
	 * Adds, to the given set, the updates of the fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param selector a selector on the fields that are being considered
	 * @param chargeGasForCPU what to apply to charge gas for CPU usage
	 * @param updates the set where they must be added
	 */
	private void addUpdatesFor(StorageReference object, TransactionReference transaction, Predicate<Update> selector, Consumer<BigInteger> chargeGasForCPU, Set<Update> updates) {
		chargeGasForCPU.accept(getGasCostModel().cpuCostForGettingResponseAt(transaction));
		TransactionResponse response = getResponseUncommittedAt(transaction);
		if (response instanceof TransactionResponseWithUpdates)
			((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField && update.object.equals(object) && selector.test(update) && !isAlreadyIn(update, updates))
				.forEach(updates::add);
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
	private Set<Field> collectAllFieldsOf(String className, EngineClassLoader classLoader, boolean onlyEager) {
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

	private static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null) {
				logger.error("transaction rejected", e.getCause());
				throw new TransactionRejectedException(e.getCause());
			}

			logger.error("transaction rejected", e);
			throw new TransactionRejectedException(e);
		}
		catch (Throwable t) {
			logger.error("transaction rejected", t);
			throw new TransactionRejectedException(t);
		}
	}

	private static <T> T wrapInCaseOfExceptionMedium(Callable<T> what) throws TransactionRejectedException, TransactionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | TransactionException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null) {
				logger.error("transaction rejected", e.getCause());
				throw new TransactionRejectedException(e.getCause());
			}

			logger.error("transaction rejected", e);
			throw new TransactionRejectedException(e);
		}
		catch (Throwable t) {
			logger.error("transaction rejected", t);
			throw new TransactionRejectedException(t);
		}
	}

	private static <T> T wrapInCaseOfExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | CodeExecutionException | TransactionException e) {
			throw e;
		}
		catch (InternalFailureException e) {
			if (e.getCause() != null) {
				logger.error("transaction rejected", e.getCause());
				throw new TransactionRejectedException(e.getCause());
			}

			logger.error("transaction rejected", e);
			throw new TransactionRejectedException(e);
		}
		catch (Throwable t) {
			logger.error("transaction rejected", t);
			throw new TransactionRejectedException(t);
		}
	}

	/**
	 * Yields an adaptor of a callable into a jar supplier.
	 * 
	 * @param task the callable
	 * @return the jar supplier
	 */
	private JarSupplier jarSupplierFor(Callable<TransactionReference> task) {
		return new JarSupplier() {
			private volatile TransactionReference cachedGet;

			@Override
			public TransactionReference get() throws TransactionRejectedException, TransactionException {
				return cachedGet != null ? cachedGet : (cachedGet = wrapInCaseOfExceptionMedium(task));
			}
		};
	}

	/**
	 * Yields an adaptor of a callable into a code supplier.
	 * 
	 * @param <W> the return value of the callable
	 * @param task the callable
	 * @return the code supplier
	 */
	private <W extends StorageValue> CodeSupplier<W> codeSupplierFor(Callable<W> task) {
		return new CodeSupplier<>() {
			private volatile W cachedGet;

			@Override
			public W get() throws TransactionRejectedException, TransactionException {
				return cachedGet != null ? cachedGet : (cachedGet = wrapInCaseOfExceptionMedium(task));
			}
		};
	}
}