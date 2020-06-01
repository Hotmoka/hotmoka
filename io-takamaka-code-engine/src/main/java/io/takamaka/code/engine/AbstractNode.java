package io.takamaka.code.engine;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
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
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.DeserializationError;
import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.NodeWithHistory;
import io.takamaka.code.engine.internal.Deserializer;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.engine.internal.transactions.AbstractNodeWithCache;

/**
 * A generic implementation of a node.
 * Specific implementations can subclass this and implement the abstract template methods.
 */
public abstract class AbstractNode<C extends Config> extends AbstractNodeWithCache implements NodeWithHistory {
	private final static Logger logger = LoggerFactory.getLogger(AbstractNode.class);

	/**
	 * The configuration of the node.
	 */
	public final C config;

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
	 * A map that provides a semaphore for each currently executing transaction.
	 * It is used to block threads waiting for the outcome of transactions.
	 */
	private final ConcurrentMap<TransactionReference, Semaphore> semaphores = new ConcurrentHashMap<>();

	/**
	 * An executor for short background tasks.
	 */
	private final ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * The hashing algorithm for transaction requests.
	 */
	private final HashingAlgorithm<? super TransactionRequest<?>> hashingForRequests;

	/**
	 * The time spent for checking requests.
	 */
	private final AtomicLong checkTime = new AtomicLong();

	/**
	 * The time spent for delivering transactions.
	 */
	private final AtomicLong deliverTime = new AtomicLong();

	/**
	 * The array of hexadecimal digits.
	 */
	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes();

	/**
	 * The default gas model of the node.
	 */
	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	/**
	 * Builds the node.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractNode(C config) {
		try {
			this.config = config;
			this.hashingForRequests = hashingForRequests();
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

	/**
	 * Post the given request to this node. It will be scheduled, eventually, checked and delivered.
	 * 
	 * @param request the request to post
	 */
	protected abstract void postTransaction(TransactionRequest<?> request);

	/**
	 * Expands the store of this node with a transaction.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	protected abstract void expandStore(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response);

	/**
	 * Expands the store of this node with a transaction that could not be delivered since an error occurred.
	 * 
	 * @param reference the reference of the request
	 * @param request the request
	 * @param errorMessage an description of why delivering failed
	 */
	protected abstract void expandStore(TransactionReference reference, TransactionRequest<?> request, String errorMessage);

	/**
	 * Yields the hashing algorithm that must be used for hashing
	 * transaction requests into their hash.
	 * 
	 * @return the SHA256 hash of the request; subclasses may redefine
	 * @throws NoSuchAlgorithmException if the required hashing algorithm is not available in the Java installation
	 */
	protected HashingAlgorithm<? super TransactionRequest<?>> hashingForRequests() throws NoSuchAlgorithmException {
		return HashingAlgorithm.sha256(Marshallable::toByteArray);
	}

	@Override
	public final TransactionResponse getResponseUncommittedAt(TransactionReference reference) {
		try {
			return getResponseAtCache.computeIfAbsent(reference, this::getResponseUncommitted);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> signatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		// we do not take into account the signature itself
		return SignatureAlgorithm.sha256dsa(NonInitialTransactionRequest::toByteArrayWithoutSignature);
	}

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param <T> the type of the result of the task
	 * @param task the task
	 * @return the return value computed by the task
	 */
	public final <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param task the task
	 * @return the return value computed by the task
	 */
	public final void submit(Runnable task) {
		executor.submit(task);
	}

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 */
	public final void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		TransactionReference reference = referenceOf(request);

		try {
			logger.info(reference + ": checking start (" + request.getClass().getSimpleName() + ')');
			request.check();
			logger.info(reference + ": checking success");
		}
		catch (TransactionRejectedException e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			expandStore(reference, request, e.getMessage());
			logger.info(reference + ": checking failed", e);
			throw e;
		}
		catch (Exception e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			expandStore(reference, request, e.getMessage());
			logger.error(reference + ": checking failed with unexpected exception", e);
			throw InternalFailureException.of(e);
		}
		finally {
			checkTime.addAndGet(System.currentTimeMillis() - start);
		}
	}

	/**
	 * Builds a response for the given request and adds it to the store of the node.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	public final void deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		TransactionReference reference = referenceOf(request);

		try {
			logger.info(reference + ": delivering start (" + request.getClass().getSimpleName() + ')');
			TransactionResponse response = ResponseBuilder.of(reference, request, this).build();
			expandStore(reference, request, response);
			logger.info(reference + ": delivering success");
		}
		catch (TransactionRejectedException e) {
			expandStore(reference, request, e.getMessage());
			logger.info(reference + ": delivering failed", e);
			throw e;
		}
		catch (Exception e) {
			expandStore(reference, request, e.getMessage());
			logger.error(reference + ": delivering failed with unexpected exception", e);
			throw InternalFailureException.of(e);
		}
		finally {
			signalSemaphore(reference);
			deliverTime.addAndGet(System.currentTimeMillis() - start);
		}
	}

	@Override
	public void close() throws Exception {
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);

		logger.info("Time spent checking requests: " + checkTime + "ms");
		logger.info("Time spent delivering requests: " + deliverTime + "ms");
	}

	/**
	 * Yields the gas cost model of this node.
	 * 
	 * @return the default gas cost model. Subclasses may redefine
	 */
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}

	@Override
	public final TransactionReference getTakamakaCode() throws NoSuchElementException {
		return getClassTag(getManifest()).jar;
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
			EngineClassLoader classLoader = new EngineClassLoader(classTag.jar, this);
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
	 */
	public final UpdateOfField getLastLazyUpdateToNonFinalField(StorageReference storageReference, FieldSignature field, Consumer<BigInteger> chargeForCPU) {
		for (TransactionReference transaction: getHistoryWithCache(storageReference, this::getHistory).collect(Collectors.toList())) {
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
	 */
	public final UpdateOfField getLastLazyUpdateToFinalField(StorageReference object, FieldSignature field, Consumer<BigInteger> chargeForCPU) {
		// accesses directly the transaction that created the object
		return getLastUpdateFor(object, field, object.transaction, chargeForCPU).orElseThrow(() -> new DeserializationError("Did not find the last update for " + field + " of " + object));
	}

	@Override
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = referenceOf(request);
			createSemaphore(reference);
			postTransaction(request);
			return ((JarStoreInitialTransactionResponse) waitForResponse(reference)).getOutcomeAt(reference);
		});
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = referenceOf(request);
			createSemaphore(reference);
			postTransaction(request);
			return waitForResponse(reference); // result unused
		});
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = referenceOf(request);
			createSemaphore(reference);
			postTransaction(request);
			return ((GameteCreationTransactionResponse) waitForResponse(reference)).getOutcome();
		});
	}

	@Override
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = referenceOf(request);
			createSemaphore(reference);
			postTransaction(request);
			return ((GameteCreationTransactionResponse) waitForResponse(reference)).getOutcome();
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
		return wrapInCaseOfExceptionFull(() -> ResponseBuilder.ofView(referenceOf(request), request, this).build().getOutcome());
	}

	@Override
	public final StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> ResponseBuilder.ofView(referenceOf(request), request, this).build().getOutcome());
	}

	@Override
	public final JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = referenceOf(request);
			createSemaphore(reference);
			postTransaction(request);
			return jarSupplierFor(() -> ((JarStoreTransactionResponse) waitForResponse(reference)).getOutcomeAt(reference));
		});
	}

	@Override
	public final CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = referenceOf(request);
			createSemaphore(reference);
			postTransaction(request);
			return codeSupplierFor(() -> ((ConstructorCallTransactionResponse) waitForResponse(reference)).getOutcome());
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = referenceOf(request);
			createSemaphore(reference);
			postTransaction(request);
			return codeSupplierFor(() -> ((MethodCallTransactionResponse) waitForResponse(reference)).getOutcome());
		});
	}

	@Override
	public final CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = referenceOf(request);
			createSemaphore(reference);
			postTransaction(request);
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

	/**
	 * Yields the reference to the translation that would be originated for the given request.
	 * 
	 * @param request the request
	 * @return the transaction reference
	 */
	private LocalTransactionReference referenceOf(TransactionRequest<?> request) {
		return new LocalTransactionReference(bytesToHex(hashingForRequests.hash(request)));
	}

	/**
	 * A cached version of {@linkplain #getHistory(StorageReference)}.
	 * 
	 * @param object the object whose history must be looked for
	 * @param getHistory the function to call in case of cache miss
	 * @return the transactions that compose the history of {@code object}, as an ordered stream
	 *         (from newest to oldest)
	 */
	Stream<TransactionReference> getHistoryWithCache(StorageReference object, Function<StorageReference, Stream<TransactionReference>> getHistory) {
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
	void setHistoryWithCache(StorageReference object, List<TransactionReference> history, BiConsumer<StorageReference, Stream<TransactionReference>> setHistory) {
		TransactionReference[] historyAsArray = history.toArray(new TransactionReference[history.size()]);
		setHistory.accept(object, history.stream());
		historyCache.put(object, historyAsArray);
	}

	/**
	 * Creates a semaphore for those who will wait for the result of the given request.
	 * 
	 * @param reference the reference of the transaction for the request
	 */
	private void createSemaphore(TransactionReference reference) {
		if (semaphores.putIfAbsent(reference, new Semaphore(0)) != null)
			throw new InternalFailureException("repeated request");
	}

	/**
	 * Wakes up who was waiting for the outcome of the given transaction.
	 * 
	 * @param reference the reference of the transaction
	 */
	private void signalSemaphore(TransactionReference reference) {
		Semaphore semaphore = semaphores.remove(reference);
		if (semaphore != null)
			semaphore.release();
	}

	/**
	 * Waits until a transaction has been committed, or until its delivering fails.
	 * If this method succeeds and this node has some form of commit, then the
	 * transaction has been definitely committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response computed for {@code request}
	 * @throws TransactionRejectedException if the request failed to be committed, because of this exception
	 * @throws TimeoutException if the polling delay has expired but the request did not get committed
	 * @throws InterruptedException if the current thread has been interrupted while waiting for the response
	 */
	private TransactionResponse waitForResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		try {
			Semaphore semaphore = semaphores.get(reference);
			if (semaphore != null)
				semaphore.acquire();

			for (int attempt = 1, delay = config.pollingDelay; attempt <= Math.max(1, config.maxPollingAttempts); attempt++, delay = delay * 110 / 100)
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