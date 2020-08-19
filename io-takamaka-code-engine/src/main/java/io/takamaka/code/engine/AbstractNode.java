package io.takamaka.code.engine;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
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
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
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
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.AbstractNodeWithSuppliers;
import io.hotmoka.nodes.DeserializationError;
import io.takamaka.code.engine.internal.transactions.ConstructorCallResponseBuilder;
import io.takamaka.code.engine.internal.transactions.GameteCreationResponseBuilder;
import io.takamaka.code.engine.internal.transactions.InitializationResponseBuilder;
import io.takamaka.code.engine.internal.transactions.InstanceMethodCallResponseBuilder;
import io.takamaka.code.engine.internal.transactions.InstanceViewMethodCallResponseBuilder;
import io.takamaka.code.engine.internal.transactions.JarStoreInitialResponseBuilder;
import io.takamaka.code.engine.internal.transactions.JarStoreResponseBuilder;
import io.takamaka.code.engine.internal.transactions.RedGreenGameteCreationResponseBuilder;
import io.takamaka.code.engine.internal.transactions.StaticMethodCallResponseBuilder;
import io.takamaka.code.engine.internal.transactions.StaticViewMethodCallResponseBuilder;
import io.takamaka.code.instrumentation.StandardGasCostModel;
import io.takamaka.code.verification.IncompleteClasspathError;

/**
 * A generic implementation of a node.
 * Specific implementations can subclass this and implement the abstract template methods.
 */
@ThreadSafe
public abstract class AbstractNode<C extends Config, S extends Store> extends AbstractNodeWithSuppliers {
	protected final static Logger logger = LoggerFactory.getLogger(AbstractNode.class);

	/**
	 * The configuration of the node.
	 */
	public final C config;

	/**
	 * The store of the node.
	 */
	private final S store;

	/**
	 * A map that provides a semaphore for each currently executing transaction.
	 * It is used to block threads waiting for the outcome of transactions.
	 */
	private final ConcurrentMap<TransactionReference, Semaphore> semaphores;

	/**
	 * The cache for the class loaders.
	 */
	private final LRUCache<TransactionReference, EngineClassLoader> classLoadersCache;

	/**
	 * The cache for the requests.
	 */
	private final LRUCache<TransactionReference, TransactionRequest<?>> requestsCache;

	/**
	 * The cache for the responses.
	 */
	private final LRUCache<TransactionReference, TransactionResponse> responsesCache;

	/**
	 * An executor for short background tasks.
	 */
	private final ExecutorService executor;

	/**
	 * The time spent for checking requests.
	 */
	private final AtomicLong checkTime;

	/**
	 * The time spent for delivering transactions.
	 */
	private final AtomicLong deliverTime;

	/**
	 * The hashing algorithm for transaction requests.
	 */
	private final HashingAlgorithm<? super TransactionRequest<?>> hashingForRequests;

	/**
	 * True if this blockchain has been already closed. Used to avoid double-closing in the shutdown hook.
	 */
	private final AtomicBoolean closed;

	/**
	 * The string of the hexadecimal digits.
	 */
	private final static String HEX_CHARS = "0123456789abcdef";

	/**
	 * The array of hexadecimal digits.
	 */
	private final static byte[] HEX_ARRAY = HEX_CHARS.getBytes();

	/**
	 * The default gas model of the node.
	 */
	private final static GasCostModel defaultGasCostModel = new StandardGasCostModel();

	/**
	 * Builds the node.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractNode(C config) {
		try {
			this.classLoadersCache = new LRUCache<>(100, 1000);
			this.requestsCache = new LRUCache<>(100, config.requestCacheSize);
			this.responsesCache = new LRUCache<>(100, config.responseCacheSize);
			this.executor = Executors.newCachedThreadPool();
			this.config = config;
			this.hashingForRequests = hashingForRequests();
			this.semaphores = new ConcurrentHashMap<>();
			this.checkTime = new AtomicLong();
			this.deliverTime = new AtomicLong();
			this.closed = new AtomicBoolean();

			if (config.delete) {
				deleteRecursively(config.dir);  // cleans the directory where the node's data live
				Files.createDirectories(config.dir);
			}

			this.store = mkStore();
			addShutdownHook();
		}
		catch (Exception e) {
			logger.error("failed to create the node", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Builds a shallow clone of the given node.
	 * 
	 * @param parent the node to clone
	 */
	protected AbstractNode(AbstractNode<C,S> parent) {
		this.classLoadersCache = parent.classLoadersCache;
		this.requestsCache = parent.requestsCache;
		this.responsesCache = parent.responsesCache;
		this.executor = parent.executor;
		this.config = parent.config;
		this.store = mkStore();
		this.hashingForRequests = parent.hashingForRequests;
		this.semaphores = parent.semaphores;
		this.checkTime = parent.checkTime;
		this.deliverTime = parent.deliverTime;
		this.closed = parent.closed;
	}

	/**
	 * Factory method for creating the store of this node.
	 * 
	 * @return the store
	 */
	protected abstract S mkStore();

	/**
	 * Determines if this node has not been closed yet.
	 * This thread-safe method can be called to avoid double-closing of a node.
	 * 
	 * @return true if and only if the node has not been closed yet
	 */
	protected final boolean isNotYetClosed() {
		return !closed.getAndSet(true);
	}

	/**
	 * Clears the caches of this node.
	 */
	protected void invalidateCaches() {
		classLoadersCache.clear();
		requestsCache.clear();
		responsesCache.clear();
	}

	/**
	 * Yields the store of this node.
	 * 
	 * @return the store of this node
	 */
	public final S getStore() {
		return store;
	}

	@Override
	public void close() throws Exception {
		S store = this.store;
		if (store != null)
			store.close();

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

	/**
	 * Yields the class loader for the given class path, using a cache to avoid
	 * regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws Exception if the class loader cannot be created
	 */
	public final EngineClassLoader getCachedClassLoader(TransactionReference classpath) throws Exception {
		return classLoadersCache.computeIfAbsent(classpath, _classpath -> new EngineClassLoader(_classpath, this));
	}

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param <T> the type of the result of the task
	 * @param task the task
	 * @return the value computed by the task
	 */
	public final <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param task the task
	 */
	public final void submit(Runnable task) {
		executor.submit(task);
	}

	/**
	 * Yields the algorithm used to sign non-initial requests with this node.
	 * 
	 * @return the ED25519 algorithm for signing non-initial requests (without their signature itself); subclasses may redefine
	 * @throws NoSuchAlgorithmException if the required signature algorithm is not available in the Java installation
	 */
	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		// we do not take into account the signature itself
		return SignatureAlgorithm.ed25519(NonInitialTransactionRequest::toByteArrayWithoutSignature);
	}

	@Override
	public final TransactionReference getTakamakaCode() throws NoSuchElementException {
		return getClassTag(getManifest()).jar;
	}

	@Override
	public final StorageReference getManifest() throws NoSuchElementException {
		return store.getManifest().orElseThrow(() -> new NoSuchElementException("no manifest set for this node"));
	}

	@Override
	public final TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		try {
			checkTransactionReference(reference);
			Semaphore semaphore = semaphores.get(reference);
			if (semaphore != null)
				semaphore.acquire();
	
			for (int attempt = 1, delay = config.pollingDelay; attempt <= Math.max(1, config.maxPollingAttempts); attempt++, delay = delay * 110 / 100)
				try {
					// we enforce that both request and response are available
					getRequest(reference);
					return getResponse(reference);
				}
				catch (NoSuchElementException e) {
					Thread.sleep(delay);
				}
	
			throw new TimeoutException("cannot find the response of transaction reference " + reference + ": tried " + config.maxPollingAttempts + " times");
		}
		catch (TransactionRejectedException | TimeoutException | InterruptedException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public final TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
		try {
			return requestsCache.computeIfAbsent(reference, _reference -> {
				checkTransactionReference(_reference);
				return store.getRequest(_reference)
					.orElseThrow(() -> new NoSuchElementException("unknown transaction reference " + _reference));
			});
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
	public final TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		try {
			return responsesCache.computeIfAbsent(reference, _reference -> {
				checkTransactionReference(_reference);
				Optional<String> error = store.getError(_reference);
				if (error.isPresent())
					throw new TransactionRejectedException(error.get());
				else
					return store.getResponse(_reference)
						.orElseThrow(() -> new NoSuchElementException("unknown transaction reference " + reference));
			});
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
			checkTransactionReference(reference.transaction);

			// we go straight to the transaction that created the object
			TransactionResponse response;
			try {
				response = getResponse(reference.transaction);
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
			checkTransactionReference(reference.transaction);
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
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = post(request);
			return ((JarStoreInitialTransactionResponse) getPolledResponse(reference)).getOutcomeAt(reference);
		});
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		wrapInCaseOfExceptionSimple(() -> getPolledResponse(post(request))); // result unused
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> ((GameteCreationTransactionResponse) getPolledResponse(post(request))).getOutcome());
	}

	@Override
	public final StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> ((GameteCreationTransactionResponse) getPolledResponse(post(request))).getOutcome());
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
	public final StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> {
			TransactionReference reference = referenceOf(request);
			logger.info(reference + ": running start (" + request.getClass().getSimpleName() + ')');
			StorageValue result = new InstanceViewMethodCallResponseBuilder(reference, request, this).getResponse().getOutcome();
			logger.info(reference + ": running success");
			return result;
		});
	}

	@Override
	public final StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> {
			TransactionReference reference = referenceOf(request);
			logger.info(reference + ": running start (" + request.getClass().getSimpleName() + ')');
			StorageValue result = new StaticViewMethodCallResponseBuilder(reference, request, this).getResponse().getOutcome();
			logger.info(reference + ": running success");
			return result;
		});
	}

	@Override
	public final JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> jarSupplierFor(post(request)));
	}

	@Override
	public final CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> constructorSupplierFor(post(request)));
	}

	@Override
	public final CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(post(request)));
	}

	@Override
	public final CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> methodSupplierFor(post(request)));
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
			logger.info(reference + ": checking start");
			request.check();
			logger.info(reference + ": checking half success");
			ResponseBuilder<?, ?> builder = responseBuilderFor(reference, request);
			logger.info(reference + ": checking success");
		}
		catch (TransactionRejectedException e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			store.push(reference, request, trimmedMessage(e));
			logger.info(reference + ": checking failed", e);
			throw e;
		}
		catch (Exception e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			store.push(reference, request, trimmedMessage(e));
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
			logger.info(reference + ": delivering start");
			TransactionResponse response = responseBuilderFor(reference, request).getResponse();
			store.push(reference, request, response);
			logger.info(reference + ": delivering success");
		}
		catch (TransactionRejectedException e) {
			store.push(reference, request, trimmedMessage(e));
			logger.info(reference + ": delivering failed", e);
			throw e;
		}
		catch (Exception e) {
			store.push(reference, request, trimmedMessage(e));
			logger.error(reference + ": delivering failed with unexpected exception", e);
			throw InternalFailureException.of(e);
		}
		finally {
			signalSemaphore(reference);
			deliverTime.addAndGet(System.currentTimeMillis() - start);
		}
	}

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

	/**
	 * Yields the builder of a response for a request of a transaction.
	 * This method can be redefined in subclasses in order to accomodate
	 * new kinds of transactions, specific to a node.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected ResponseBuilder<?,?> responseBuilderFor(TransactionReference reference, TransactionRequest<?> request) throws TransactionRejectedException {
		if (request instanceof JarStoreInitialTransactionRequest)
			return new JarStoreInitialResponseBuilder(reference, (JarStoreInitialTransactionRequest) request, this);
		else if (request instanceof RedGreenGameteCreationTransactionRequest)
			return new RedGreenGameteCreationResponseBuilder(reference, (RedGreenGameteCreationTransactionRequest) request, this);
    	else if (request instanceof GameteCreationTransactionRequest)
    		return new GameteCreationResponseBuilder(reference, (GameteCreationTransactionRequest) request, this);
    	else if (request instanceof JarStoreTransactionRequest)
    		return new JarStoreResponseBuilder(reference, (JarStoreTransactionRequest) request, this);
    	else if (request instanceof ConstructorCallTransactionRequest)
    		return new ConstructorCallResponseBuilder(reference, (ConstructorCallTransactionRequest) request, this);
    	else if (request instanceof InstanceMethodCallTransactionRequest)
    		return new InstanceMethodCallResponseBuilder(reference, (InstanceMethodCallTransactionRequest) request, this);
    	else if (request instanceof StaticMethodCallTransactionRequest)
    		return new StaticMethodCallResponseBuilder(reference, (StaticMethodCallTransactionRequest) request, this);
    	else if (request instanceof InitializationTransactionRequest)
    		return new InitializationResponseBuilder(reference, (InitializationTransactionRequest) request, this);
    	else
    		throw new TransactionRejectedException("unexpected transaction request of class " + request.getClass().getName());
	}

	/**
	 * Posts the given request. It does some preliminary preparation then calls
	 * {@link #postRequest(TransactionRequest)}, that will implement the node-specific
	 * logic of this post.
	 * 
	 * @param request the request
	 * @return the reference of the request
	 */
	protected final TransactionReference post(TransactionRequest<?> request) {
		TransactionReference reference = referenceOf(request);
		logger.info(reference + ": posting (" + request.getClass().getSimpleName() + ')');
		createSemaphore(reference);
		postRequest(request);
	
		return reference;
	}

	/**
	 * Node-specific implementation to post the given request. Each node should implement this,
	 * for instance by adding the request to some mempool or queue of requests to be executed.
	 * 
	 * @param request the request
	 */
	protected abstract void postRequest(TransactionRequest<?> request);

	/**
	 * Yields the reference to the translation that would be originated for the given request.
	 * 
	 * @param request the request
	 * @return the transaction reference
	 */
	protected final LocalTransactionReference referenceOf(TransactionRequest<?> request) {
		return new LocalTransactionReference(bytesToHex(hashingForRequests.hash(request)));
	}

	private void checkTransactionReference(TransactionReference reference) {
		// each byte is represented by two successive characters
		String hash;

		if (reference == null || (hash = reference.getHash()) == null || hash.length() != hashingForRequests.length() * 2)
			throw new IllegalArgumentException("illegal transaction reference " + reference + ": it should hold a hash of " + hashingForRequests.length() * 2 + " characters");

		if (hash.chars().anyMatch(c -> HEX_CHARS.indexOf(c) == -1))
			throw new IllegalArgumentException("illegal transaction reference " + reference + ": only \"" + HEX_CHARS + "\" are allowed");
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
     * Yields the error message trimmed to a maximal length, to avoid overflow.
     *
     * @param t the throwable whose error message is processed
     * @return the resulting message
     */
	private String trimmedMessage(Throwable t) {
    	String message = t.getMessage();
		int length = message.length();
		if (length > config.maxErrorLength)
			return message.substring(0, config.maxErrorLength) + "...";
		else
			return message;
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
		TransactionResponse response = store.getResponseUncommitted(transaction)
			.orElseThrow(() -> new DeserializationError("Unknown transaction reference " + transaction));

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
		collectUpdatesFor(object, store.getHistory(object), updates, allFields.size());
	
		return updates.stream();
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
	 * Adds, to the given set, the updates of the fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param updates the set where they must be added
	 */
	private void addUpdatesFor(StorageReference object, TransactionReference transaction, Set<Update> updates) {
		try {
			TransactionResponse response = getResponse(transaction);
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