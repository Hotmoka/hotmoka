package io.takamaka.code.engine;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.Classpath;
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
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.Deserializer;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.engine.internal.transactions.AbstractNodeWithCache;

/**
 * A generic implementation of a node.
 * Specific implementations can subclass this and implement the abstract template methods.
 */
public abstract class AbstractNode<C extends Config> extends AbstractNodeWithCache implements Node {

	/**
	 * The configuration of the node.
	 */
	private final C config;
	
	/**
	 * The cache for the {@linkplain #getRequestAt(TransactionReference)} method.
	 */
	private final LRUCache<TransactionReference, TransactionRequest<?>> getRequestAtCache;

	/**
	 * The cache for the {@linkplain #getResponseAt(TransactionReference)} method.
	 */
	private final LRUCache<TransactionReference, TransactionResponse> getResponseAtCache;

	/**
	 * The array of hexadecimal digits.
	 */
	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes();

	/**
	 * A cache where {@linkplain #checkTransaction(TransactionRequest)} stores the builders and
	 * from where {@linkplain #deliverTransaction(TransactionRequest)} can retrieve them
	 * (instead of recreating them, when not found).
	 */
	private final LRUCache<TransactionRequest<?>, ResponseBuilder<?,?>> builders;

	/**
	 * A cache for {@linkplain #getHistory(StorageReference)}.
	 */
	private final LRUCache<StorageReference, TransactionReference[]> historyCache;

	/**
	 * The default gas model of the node.
	 */
	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	/**
	 * An executor for short background tasks.
	 */
	private final ExecutorService executor = Executors.newCachedThreadPool();

	private final ConcurrentMap<TransactionRequest<?>, Semaphore> semaphores = new ConcurrentHashMap<>();

	/**
	 * The time spent for checking requests.
	 */
	private long checkTime;

	/**
	 * The time spent for delivering transactions.
	 */
	private long deliverTime;

	private final static Logger logger = LoggerFactory.getLogger(AbstractNode.class);

	/**
	 * Builds the node.
	 * 
	 * @param config the configuration of the node
	 * @throws NoSuchAlgorithmException if the default hashing algorithm for requests cannot be found
	 */
	protected AbstractNode(C config) throws NoSuchAlgorithmException {
		this.config = config;
		this.getRequestAtCache = new LRUCache<>(config.requestCacheSize);
		this.getResponseAtCache = new LRUCache<>(config.responseCacheSize);
		this.builders = new LRUCache<>(config.builderCacheSize);
		this.historyCache = new LRUCache<>(config.historyCacheSize);
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
	 *         (from newest to oldest). If {@code object} has currently no history, it can yield an
	 *         empty stream, but never throw an exception
	 */
	protected abstract Stream<TransactionReference> getHistory(StorageReference object);

	/**
	 * Sets the history of the given object, that is,
	 * the references to the transactions that might have updated the
	 * given object, in reverse chronological order (from newest to oldest).
	 * This stream is an over-approximation, hence it might also contain transactions
	 * that did not affect the object, but it must include all that did it.
	 * 
	 * @param object the object whose history is set
	 * @param history the stream that will become the history of the object,
	 *                replacing its previous history
	 */
	protected abstract void setHistory(StorageReference object, Stream<TransactionReference> history);

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * The result of this method is wrapped into a cache in order to implement
	 * {@linkplain #getRequestAt(TransactionReference)}.
	 * 
	 * @param transactionReference the reference of the transaction
	 * @return the request
	 * @throws NoSuchElementException if the request cannot be found
	 */
	protected abstract TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException;

	/**
	 * Yields the response generated by the transaction with the given reference.
	 * The result of this method is wrapped into a cache in order to implement
	 * {@linkplain #getResponseAt(TransactionReference)}.
	 * 
	 * @param transactionReference the reference to the transaction
	 * @return the response
	 * @throws TransactionRejectedException if an attempt to execute the transaction generated this exception
	 * @throws NoSuchElementException if the response cannot be found
	 */
	protected abstract TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException;

	/**
	 * Post the given request. It will be scheduled, eventually, and executed.
	 * 
	 * @param request the request that gets posted
	 */
	protected abstract void postTransaction(TransactionRequest<?> request);

	/**
	 * Yields the hash of the given request.
	 * 
	 * @param request the request
	 * @return the SHA256 hash of the request; subclasses may redefine
	 */
	protected String hash(TransactionRequest<?> request) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return bytesToHex(digest.digest(request.toByteArray()));
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	private static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }

	    return new String(hexChars, StandardCharsets.UTF_8);
	}

	/**
	 * Yields the transaction reference to the translation that would be originated for
	 * the given request.
	 * 
	 * @param request the request
	 * @return the transaction reference
	 */
	public final LocalTransactionReference referenceOf(TransactionRequest<?> request) {
		return new LocalTransactionReference(hash(request));
	}

	/**
	 * Expands the store of this node with a transaction. If this method is redefined in
	 * subclasses, such redefinitions should call into this at the end.
	 * 
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	protected void expandStore(TransactionRequest<?> request, TransactionResponse response) {
		TransactionReference reference = referenceOf(request);
		if (response instanceof TransactionResponseWithUpdates)
			expandHistory(reference, (TransactionResponseWithUpdates) response);

		getResponseAtCache.put(reference, response);
	}

	/**
	 * Yields the configuration of this node.
	 * 
	 * @return the configuration of this node
	 */
	public final C getConfig() {
		return config;
	}

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
	 * @param what the task
	 * @return the return value computed by the task
	 */
	public final void submit(Runnable what) {
		executor.submit(what);
	}

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
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	public final void deliverTransaction(ResponseBuilder<?,?> builder) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		TransactionRequest<?> request = builder.getRequest();
		TransactionResponse response = builder.build();
		expandStore(request, response);
	
		deliverTime += (System.currentTimeMillis() - start);
	}

	/**
	 * Takes note that the transaction for the given request did not manage to be correctly delivered
	 * into the store of the node.
	 * 
	 * @param request the request
	 * @param errorMessage an description of why delivering failed
	 */
	public void notifyTransactionUndelivered(TransactionRequest<?> request, String errorMessage) {
		Semaphore semaphore = semaphores.remove(request);
		if (semaphore != null)
			semaphore.release();
	}

	/**
	 * Takes note that the transaction for the given request has been correctly delivered
	 * into the store of the node.
	 * 
	 * @param request the request
	 */
	public void notifyTransactionDelivered(TransactionRequest<?> request) {
		Semaphore semaphore = semaphores.remove(request);
		if (semaphore != null)
			semaphore.release();
	}

	@Override
	public void close() throws Exception {
		executor.shutdown();

		logger.info("Time spent checking requests: " + checkTime + "ms");
		logger.info("Time spent delivering requests: " + deliverTime + "ms");
	}

	/**
	 * Yields the gas cost model of this node.
	 * 
	 * @return the gas cost model
	 */
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}

	@Override
	public final TransactionRequest<?> getRequestAt(TransactionReference reference) throws NoSuchElementException {
		try {
			TransactionRequest<?> request = getRequestAtCache.get(reference);
			if (request == null) {
				request = getRequest(reference);
				if (request != null)
					getRequestAtCache.put(reference, request);
			}

			return request;
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
			TransactionResponse response = getResponseAtCache.get(reference);
			if (response == null) {
				response = getResponse(reference);
				if (response != null)
					getResponseAtCache.put(reference, response);
				else
					throw new NoSuchElementException("unknown transaction reference " + reference);
			}

			return response;
		}
		catch (TransactionRejectedException | NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the class tag of the object with the given storage reference.
	 * 
	 * @param reference the storage reference
	 * @return the class tag, if any
	 * @throws NoSuchElementException if the class tag could not be found
	 */
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
			EngineClassLoader classLoader = new EngineClassLoader(new Classpath(classTag.jar, true), this);
			Deserializer deserializer = new Deserializer(this, classLoader);
			return deserializer.getLastUpdates(reference);
		}
		catch (NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the most recent update for the given non-{@code final} field,
	 * of lazy type, of the object with the given storage reference.
	 * 
	 * @param storageReference the storage reference
	 * @param field the field whose update is being looked for
	 * @param chargeForCPU a function called to charge CPU costs
	 * @return the update
	 * @throws Exception if the update could not be found
	 */
	public final UpdateOfField getLastLazyUpdateToNonFinalField(StorageReference storageReference, FieldSignature field, Consumer<BigInteger> chargeForCPU) {
		for (TransactionReference transaction: getHistoryWithCache(storageReference).collect(Collectors.toList())) {
			Optional<UpdateOfField> update = getLastUpdateFor(storageReference, field, transaction, chargeForCPU);
			if (update.isPresent())
				return update.get();
		}

		throw new DeserializationError("did not find the last update for " + field + " of " + storageReference);
	}

	/**
	 * Yields the most recent update for the given {@code final} field,
	 * of lazy type, of the object with the given storage reference.
	 * Its implementation can be identical to
	 * that of {@link #getLastLazyUpdateToNonFinalField(StorageReference, FieldSignature, Consumer<BigInteger>)},
	 * or instead exploit the fact that the field is {@code final}, for an optimized look-up.
	 * 
	 * @param storageReference the storage reference
	 * @param field the field whose update is being looked for
	 * @param chargeForCPU a function called to charge CPU costs
	 * @return the update
	 * @throws Exception if the update could not be found
	 */
	public final UpdateOfField getLastLazyUpdateToFinalField(StorageReference object, FieldSignature field, Consumer<BigInteger> chargeForCPU) {
		// accesses directly the transaction that created the object
		return getLastUpdateFor(object, field, object.transaction, chargeForCPU).orElseThrow(() -> new DeserializationError("Did not find the last update for " + field + " of " + object));
	}

	/**
	 * Expands the store of this node with a transaction that
	 * installs a jar in it. It has no caller and requires no gas. The goal is to install, in the
	 * node, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes.
	 * This installation have special privileges, such as that of installing
	 * packages in {@code io.takamaka.code.lang.*}.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			postTransaction(request);
			return ((JarStoreInitialTransactionResponse) waitForResponse(request)).getOutcomeAt(referenceOf(request));
		});
	}

	/**
	 * Expands the store of this node with a transaction that creates a gamete, that is,
	 * an externally owned contract with the given initial amount of coins.
	 * This transaction has no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			postTransaction(request);
			return ((GameteCreationTransactionResponse) waitForResponse(request)).getOutcome();
		});
	}

	/**
	 * Expands the store of this node with a transaction that creates a red/green gamete, that is,
	 * a red/green externally owned contract with the given initial amount of coins.
	 * This transaction has no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			postTransaction(request);
			return ((GameteCreationTransactionResponse) waitForResponse(request)).getOutcome();
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
		return wrapInCaseOfExceptionFull(() -> ResponseBuilder.ofView(request, this).build().getOutcome());
	}

	@Override
	public final StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> ResponseBuilder.ofView(request, this).build().getOutcome());
	}

	@Override
	public final JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			createSemaphore(request);
			postTransaction(request);
			return jarSupplierFor(() -> {
				TransactionReference reference = referenceOf(request);
				return ((JarStoreTransactionResponse) waitForResponse(request)).getOutcomeAt(reference);
			});
		});
	}

	@Override
	public final CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			createSemaphore(request);
			postTransaction(request);
			return codeSupplierFor(() -> ((ConstructorCallTransactionResponse) waitForResponse(request)).getOutcome());
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			createSemaphore(request);
			postTransaction(request);
			return codeSupplierFor(() -> ((MethodCallTransactionResponse) waitForResponse(request)).getOutcome());
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			createSemaphore(request);
			postTransaction(request);
			return codeSupplierFor(() -> ((MethodCallTransactionResponse) waitForResponse(request)).getOutcome());
		});
	}

	/**
	 * Yields the history of the given object, that is,
	 * the references to the transactions that might have updated the
	 * given object, in reverse chronological order (from newest to oldest).
	 * This stream is an over-approximation, hence it might also contain transactions
	 * that did not affect the object, but it must include all that did it.
	 * If the node has some form of commit, this history must include also
	 * transactions executed but not yet committed.
	 * It uses a cache for repeated calls.
	 * 
	 * @param object the object whose update history must be looked for
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest). If {@code object} has currently no history, it can yield an
	 *         empty stream, but never throw an exception
	 */
	public Stream<TransactionReference> getHistoryWithCache(StorageReference object) {
		TransactionReference[] result = historyCache.get(object);
		return result != null ? Stream.of(result) : getHistory(object);
	}

	/**
	 * Sets the history of the given object, that is,
	 * the references to the transactions that might have updated the
	 * given object, in reverse chronological order (from newest to oldest).
	 * This stream is an over-approximation, hence it might also contain transactions
	 * that did not affect the object, but it must include all that did it.
	 * It puts the history in a cache for future quick look-up.
	 * 
	 * @param object the object whose history is set
	 * @param history the stream that will become the history of the object,
	 *                replacing its previous history
	 */
	private void setHistoryWithCache(StorageReference object, List<TransactionReference> history) {
		TransactionReference[] historyAsArray = history.toArray(new TransactionReference[history.size()]);
		setHistory(object, history.stream());
		historyCache.put(object, historyAsArray);
	}

	/**
	 * Process the updates contained in the given response, expanding the history of the affected objects.
	 * This method should be called at the end of a transaction, to keep in store the updates to the objects.
	 * 
	 * @param reference the transaction that has generated the given response
	 * @param response the response
	 */
	private void expandHistory(TransactionReference reference, TransactionResponseWithUpdates response) {
		// we collect the storage references that have been updated in the response; for each of them,
		// we fetch the list of the transaction references that affected them in the past, we add the new transaction reference
		// in front of such lists and store back the updated lists, replacing the old ones
		response.getUpdates()
			.map(Update::getObject)
			.distinct()
			.forEachOrdered(object -> setHistoryWithCache(object, simplifiedHistory(object, reference, response, getHistoryWithCache(object))));
	}

	private void createSemaphore(TransactionRequest<?> request) {
		if (semaphores.putIfAbsent(request, new Semaphore(0)) != null)
			throw new InternalFailureException("repeated request");
	}

	private TransactionResponse waitForResponse(TransactionRequest<?> request) throws TransactionRejectedException, TimeoutException, InterruptedException {
		try {
			Semaphore semaphore = semaphores.get(request);
			if (semaphore != null)
				semaphore.acquire();

			LocalTransactionReference reference = referenceOf(request);
			for (int attempt = 1, delay = config.pollingDelay; attempt < config.maxPollingAttempts; attempt++, delay = delay * 110 / 100)
				try {
					return getResponseAt(reference);
				}
				catch (NoSuchElementException e) {
					Thread.sleep(delay);
				}

			throw new TimeoutException("cannot find response for transaction reference " + reference + ": tried " + config.maxPollingAttempts + " times");
		}
		catch (TransactionRejectedException | TimeoutException | InterruptedException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	private List<TransactionReference> simplifiedHistory(StorageReference object, TransactionReference added, TransactionResponseWithUpdates response, Stream<TransactionReference> old) {
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
	
		return simplified;
	}

	private void addIfUseful(TransactionReference cursor, StorageReference object, Set<Update> covered, List<TransactionReference> simplified) {
		TransactionResponse response;

		try {
			response = getResponseAt(cursor);
		}
		catch (NoSuchElementException | TransactionRejectedException e) {
			logger.error("history contains a reference to a missing or failed transaction", e);
			throw new InternalFailureException("history contains a reference to a missing or failed transaction");
		}

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
	private Optional<UpdateOfField> getLastUpdateFor(StorageReference object, FieldSignature field, TransactionReference transaction, Consumer<BigInteger> chargeForCPU) {
		chargeForCPU.accept(getGasCostModel().cpuCostForGettingResponseAt(transaction));

		TransactionResponse response;
		try {
			response = getResponseAt(transaction);
		}
		catch (NoSuchElementException | TransactionRejectedException e) {
			return Optional.empty();
		}

		if (response instanceof TransactionResponseWithUpdates)
			return ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(object) && update.getField().equals(field))
				.findAny();
	
		return Optional.empty();
	}

	private static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException e) {
			throw e;
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
		catch (Throwable t) {
			logger.error("transaction rejected", t);
			throw new TransactionRejectedException(t);
		}
	}

	private JarSupplier jarSupplierFor(Callable<TransactionReference> task) {
		return new JarSupplier() {
			private volatile TransactionReference cachedGet;

			@Override
			public TransactionReference get() throws TransactionRejectedException, TransactionException {
				if (cachedGet != null)
					return cachedGet;

				try {
					return cachedGet = task.call();
				}
				catch (TransactionRejectedException | TransactionException e) {
					throw e;
				}
				catch (Throwable t) {
					logger.error("transaction rejected", t);
					throw new TransactionRejectedException(t);
				}
			}
		};
	}

	private <W extends StorageValue> CodeSupplier<W> codeSupplierFor(Callable<W> task) {
		return new CodeSupplier<>() {
			private volatile W cachedGet;

			@Override
			public W get() throws TransactionRejectedException, TransactionException {
				if (cachedGet != null)
					return cachedGet;

				try {
					return cachedGet = task.call();
				}
				catch (TransactionRejectedException | TransactionException e) {
					throw e;
				}
				catch (Throwable t) {
					logger.error("transaction rejected", t);
					throw new TransactionRejectedException(t);
				}
			}
		};
	}
}