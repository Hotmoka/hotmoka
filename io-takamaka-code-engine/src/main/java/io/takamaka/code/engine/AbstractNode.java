package io.takamaka.code.engine;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.LocalTransactionReference;
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
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.transactions.AbstractNodeWithCache;

/**
 * A generic implementation of a node.
 * Specific implementations can subclass this class and implement the abstract template methods.
 */
public abstract class AbstractNode extends AbstractNodeWithCache implements Node {

	/**
	 * The size of the cache for the {@linkplain #getResponseAt(TransactionReference)} method.
	 */
	private static final int GET_RESPONSE_CACHE_SIZE = 1000;

	private final LRUCache<TransactionReference, TransactionResponse> getResponseAtCache = new LRUCache<>(1000, GET_RESPONSE_CACHE_SIZE);

	/**
	 * The maximal number of polling attempts, while waiting for the result of a posted transaction.
	 */
	public final static int MAX_POLLING_ATTEMPTS = 100;

	/**
	 * The delay of two subsequent polling attempts, while waiting for the result of a posted transaction.
	 * This delay is then increased by 10% at each subsequent attempt.
	 */
	public final static int POLLING_DELAY = 10;

	/**
	 * A cache where {@linkplain #checkTransaction(TransactionRequest)} stores the builders and
	 * from where {@linkplain #deliverTransaction(TransactionRequest)} can retrieve them.
	 */
	private final LRUCache<TransactionRequest<?>, ResponseBuilder<?,?>> builders = new LRUCache<>(10_000);

	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	private final ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * The reference that identifies the next transaction that will be executed with this node.
	 */
	private TransactionReference next = LocalTransactionReference.FIRST;

	/**
	 * The lock that guards accesses to the {@code next} field.
	 */
	private final Object lockGetNext = new Object();

	/**
	 * Sets the reference that will be used to refer to the next transaction that will be executed
	 * with this node.
	 * 
	 * @param next the reference
	 */
	protected void setNext(TransactionReference next) {
		synchronized (lockGetNext) {
			this.next = next;
		}
	}

	/**
	 * This method is called when a view transaction gets scheduled for execution.
	 * It must yield a transaction reference that can be used to reference the
	 * scheduled transaction at that moment. This method must be
	 * thread-safe, that is, more thread must be able to call into it.
	 * 
	 * @return the transaction reference
	 */
	protected final TransactionReference next() {
		synchronized (lockGetNext) {
			return next;
		}
	}

	/**
	 * This method is called when a non-view transaction gets scheduled for execution.
	 * It must yield a transaction reference that can be used to reference the
	 * scheduled transaction at that moment. It is guaranteed that next time this
	 * method will be called, a different reference will be returned, never seen
	 * before. This method must be thread-safe, that is, more thread must be able to call into it.
	 * 
	 * @return the transaction reference
	 */
	protected TransactionReference nextAndIncrement() {
		TransactionReference result;
	
		synchronized (lockGetNext) {
			result = next;
			next = next.getNext();
			setNext(next);
		}
	
		return result;
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
	 *         (from newest to oldest). If {@code object} has currently no history, can yield an
	 *         empty stream, but never throw an exception
	 */
	protected abstract Stream<TransactionReference> getHistoryOf(StorageReference object);

	/**
	 * Sets the history of the given object, that is,
	 * the references to the transactions that might have updated the
	 * given object, in reverse chronological order (from newest to oldest).
	 * This stream is an over-approximation, hence it might also contain transactions
	 * that did not affect the object, but it must include all that did it.
	 * 
	 * @param object the object whose history is set
	 * @param history the stream that will become the history of the object,
	 *                replacing its previous history, if any
	 */
	protected abstract void setHistory(StorageReference object, Stream<TransactionReference> history);

	/**
	 * Process the updates contained in the given response, expanding the history of the affected objects.
	 * This method should be called at the end of a transaction, to keep in store the updates to the objects.
	 * 
	 * @param transactionReference the transaction that has generated the given response
	 * @param response the response
	 * @throws Exception if the history could not be expanded
	 */
	private void expandHistoryWith(TransactionReference transactionReference, TransactionResponseWithUpdates response) throws Exception {
		// we collect the storage references that have been updated in the response; for each of them,
		// we fetch the list of the transaction references that affected them in the past, we add the new transaction reference
		// in front of such lists and store back the updated lists, replacing the old ones
		Stream<StorageReference> affectedObjects = response.getUpdates()
			.map(Update::getObject)
			.distinct();
	
		for (StorageReference object: affectedObjects.toArray(StorageReference[]::new))
			setHistory(object, simplifiedHistoryOf(object, transactionReference, response, getHistoryOf(object)));
	}

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

	/**
	 * Yields the response generated by the transaction with the given reference.
	 * It is an internal method, wrapped into a cache in order to implement
	 * {@linkplain #getResponseAt(TransactionReference)}.
	 * 
	 * @param transactionReference the reference to the transaction
	 * @return the response
	 * @throws Exception if the response cannot be found
	 */
	protected abstract TransactionResponse getResponseAtInternal(TransactionReference reference) throws Exception;

	protected abstract Supplier<String> postTransaction(TransactionRequest<?> request) throws Exception;

	/**
	 * Expands the store of this node with a transaction. If this method is redefined in
	 * subclasses, such redefinitions should call into this at the end.
	 * 
	 * @param <Request> the type of the request of the transaction
	 * @param <Response> the type of the response of the transaction
	 * @param reference the reference for the transaction
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 * @throws Exception if the expansion cannot be completed
	 */
	protected void expandStoreWith(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws Exception {
		if (response instanceof TransactionResponseWithUpdates)
			expandHistoryWith(reference, (TransactionResponseWithUpdates) response);

		getResponseAtCache.put(reference, response);

		/*int diff1 = request.toByteArray().length;
		int diff2 = response.toByteArray().length;
		requests += diff1;
		responses += diff2;
		System.out.println("requests = " + requests + " [+" + diff1 + "]" + " and responses = " + responses + " [+" + diff2 + "]");*/
	}

	private static long requests;
	private static long responses;

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param <T> the type of the result of the task
	 * @param what the task
	 * @return the return value computed by the task
	 */
	public final <T> Future<T> submit(Callable<T> what) {
		return executor.submit(what);
	}

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param <T> the type of the result of the task
	 * @param what the task
	 * @return the return value computed by the task
	 */
	public final void submit(Runnable what) {
		executor.submit(what);
	}

	public long checkTime, deliverTime;

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @return the builder of the response
	 * @throws TransactionRejectedException if the request is not valid
	 */
	public final ResponseBuilder<?,?> checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		ResponseBuilder<?,?> builder = builders.get(request);
		if (builder == null) {
			builder = ResponseBuilder.of(request, this);
			// we store the builder where next call might be able to find it
			builders.put(request, builder);
		}

		checkTime += (System.currentTimeMillis() - start);
		return builder;
	}

	/**
	 * Uses the given response builder to build a response, adds it to the store of the
	 * node and yields the response. It guarantees that responses are computed in increasing
	 * order of reference.
	 * 
	 * @param builder the builder
	 * @param reference the reference that must be used to refer to the transaction
	 * @throws Exception if the response could not be computed or the store could not be expanded
	 */
	public final <Request extends TransactionRequest<Response>, Response extends TransactionResponse> void deliverTransaction(ResponseBuilder<Request,Response> builder, TransactionReference reference) throws Exception {
		long start = System.currentTimeMillis();

		TransactionRequest<?> request = builder.getRequest();
		Response response = builder.build(reference);
		expandStoreWith(reference, request, response);
	
		deliverTime += (System.currentTimeMillis() - start);
	}

	public final void releaseWhoWasWaitingFor(TransactionRequest<?> request) {
		Semaphore semaphore = semaphores.remove(request);
		if (semaphore != null)
			semaphore.release();
	}

	@Override
	public void close() throws Exception {
		executor.shutdown();
	}

	@Override
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}

	@Override
	public final TransactionResponse getResponseAt(TransactionReference reference) throws Exception {
		TransactionResponse response = getResponseAtCache.get(reference);
		if (response != null)
			return response;
	
		response = getResponseAtInternal(reference);
		getResponseAtCache.put(reference, response);
	
		return response;
	}

	@Override
	public final ClassTag getClassTagOf(StorageReference storageReference, Consumer<BigInteger> chargeForCPU) throws Exception {
		// we go straight to the transaction that created the object
		TransactionResponse response = getResponseAndCharge(storageReference.transaction, chargeForCPU);

		if (!(response instanceof TransactionResponseWithUpdates))
			throw new DeserializationError("Storage reference " + storageReference + " does not contain updates");

		return ((TransactionResponseWithUpdates) response).getUpdates()
			.filter(update -> update instanceof ClassTag && update.object.equals(storageReference))
			.map(update -> (ClassTag) update)
			.findFirst().get();
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
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			requireNodeUninitialized();
			String id = postTransaction(request).get();
			TransactionReference reference = getTransactionReferenceFor(id);
			return ((JarStoreInitialTransactionResponse) getResponseAt(reference)).getOutcomeAt(reference);
		});
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			requireNodeUninitialized();
			String id = postTransaction(request).get();
			return ((GameteCreationTransactionResponse) getResponseAt(getTransactionReferenceFor(id))).getOutcome();
		});
	}

	@Override
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			requireNodeUninitialized();
			String id = postTransaction(request).get();
			return ((GameteCreationTransactionResponse) getResponseAt(getTransactionReferenceFor(id))).getOutcome();
		});
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
		return wrapInCaseOfExceptionFull(() -> ResponseBuilder.ofView(request, this).build(next()).getOutcome());
	}

	@Override
	public final StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> ResponseBuilder.ofView(request, this).build(next()).getOutcome());
	}

	@Override
	public final JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			markAsInitialized();
			createSemaphoreFor(request);
			Supplier<String> id = postTransaction(request);
			return jarSupplierFor(_id -> {
				TransactionReference reference = getTransactionReferenceFor(_id);
				return ((JarStoreTransactionResponse) waitForResponseOf(request, _id)).getOutcomeAt(reference);
			}, id);
		});
	}

	@Override
	public final CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			markAsInitialized();
			createSemaphoreFor(request);
			Supplier<String> id = postTransaction(request);
			return codeSupplierFor(_id -> ((ConstructorCallTransactionResponse) waitForResponseOf(request, _id)).getOutcome(), id);
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			markAsInitialized();
			createSemaphoreFor(request);
			Supplier<String> id = postTransaction(request);
			return codeSupplierFor(_id -> ((MethodCallTransactionResponse) waitForResponseOf(request, _id)).getOutcome(), id);
		});
	}

	private void createSemaphoreFor(TransactionRequest<?> request) {
		Semaphore semaphore = new Semaphore(0);
		if (semaphores.putIfAbsent(request, semaphore) != null)
			throw new IllegalStateException("repeated request");
	}

	private final ConcurrentMap<TransactionRequest<?>, Semaphore> semaphores = new ConcurrentHashMap<>();

	private TransactionResponse waitForResponseOf(TransactionRequest<?> request, String id) throws Exception {
		Semaphore semaphore = semaphores.get(request);
		if (semaphore != null)
			semaphore.acquire();

		return getResponseAt(getTransactionReferenceFor(id));
	}

	@Override
	public final CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			markAsInitialized();
			createSemaphoreFor(request);
			Supplier<String> id = postTransaction(request);
			return codeSupplierFor(_id -> ((MethodCallTransactionResponse) waitForResponseOf(request, _id)).getOutcome(), id);
		});
	}

	private Stream<TransactionReference> simplifiedHistoryOf(StorageReference object, TransactionReference added, TransactionResponseWithUpdates response, Stream<TransactionReference> old) throws Exception {
		// we trace the set of updates that are already covered by previous transactions, so that
		// subsequent history elements might become unnecessary, since they do not add any yet uncovered update
		Set<Update> covered = response.getUpdates().filter(update -> update.getObject() == object).collect(Collectors.toSet());
		List<TransactionReference> simplified = new ArrayList<>();
		simplified.add(added);
	
		TransactionReference[] oldAsArray = old.toArray(TransactionReference[]::new);
		int length = oldAsArray.length;
		for (int pos = 0; pos < length - 1; pos++)
			addIfUseful(oldAsArray[pos], object, covered, simplified);
	
		// the last is always useful, since it contains the final fields and the class tag of the object
		if (length >= 1)
			simplified.add(oldAsArray[length - 1]);
	
		return simplified.stream();
	}

	private void addIfUseful(TransactionReference cursor, StorageReference object, Set<Update> covered, List<TransactionReference> simplified) throws Exception {
		TransactionResponse response = getResponseAt(cursor);
		if (response instanceof TransactionResponseWithUpdates) {
			Set<Update> diff = ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update.getObject().equals(object))
				.filter(update -> !isAlreadyIn(update, covered))
				.collect(Collectors.toSet());
	
			if (!diff.isEmpty()) {
				// the transaction reference actually adds at least one useful update
				simplified.add(cursor);
				covered.addAll(diff);
			}
		}
	}

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

	private static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (Throwable t) {
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
		catch (Throwable t) {
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
		catch (Throwable t) {
			throw new TransactionRejectedException(t);
		}
	}

	public interface Task<V> {
		V apply(String id) throws Exception;
	}

	private JarSupplier jarSupplierFor(Task<TransactionReference> task, Supplier<String> id) {
		return new JarSupplier() {

			private volatile String cachedId;
			private volatile TransactionReference cachedGet;

			@Override
			public TransactionReference get() throws TransactionRejectedException, TransactionException {
				if (cachedGet != null)
					return cachedGet;

				try {
					return cachedGet = task.apply(id());
				}
				catch (TransactionRejectedException | TransactionException e) {
					throw e;
				}
				catch (Throwable t) {
					throw new TransactionRejectedException(t);
				}
			}

			@Override
			public String id() throws TransactionRejectedException {
				if (cachedId != null)
					return cachedId;

				try {
					return cachedId = id.get();
				}
				catch (Throwable t) {
					throw new TransactionRejectedException(t);
				}
			}
		};
	}

	private <W extends StorageValue> CodeSupplier<W> codeSupplierFor(Task<W> task, Supplier<String> id) {
		return new CodeSupplier<>() {

			private volatile String cachedId;
			private volatile W cachedGet;

			@Override
			public W get() throws TransactionRejectedException, TransactionException {
				if (cachedGet != null)
					return cachedGet;

				try {
					return cachedGet = task.apply(id());
				}
				catch (TransactionRejectedException | TransactionException e) {
					throw e;
				}
				catch (Throwable t) {
					throw new TransactionRejectedException(t);
				}
			}

			@Override
			public String id() throws TransactionRejectedException {
				if (cachedId != null)
					return cachedId;

				try {
					return cachedId = id.get();
				}
				catch (Throwable t) {
					throw new TransactionRejectedException(t);
				}
			}
		};
	}
}