package io.takamaka.code.engine;

import static java.math.BigInteger.ZERO;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
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
import io.hotmoka.beans.requests.AbstractInstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SystemTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithEvents;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.updates.UpdateOfBigInteger;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.updates.UpdateOfInt;
import io.hotmoka.beans.updates.UpdateOfRedBalance;
import io.hotmoka.beans.updates.UpdateOfStorage;
import io.hotmoka.beans.updates.UpdateOfString;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.AbstractNode;
import io.hotmoka.nodes.DeserializationError;
import io.takamaka.code.engine.internal.LRUCache;
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
 * A generic implementation of a local (ie., non-remote) node.
 * Specific implementations can subclass this and implement the abstract template methods.
 */
@ThreadSafe
public abstract class AbstractLocalNode<C extends Config, S extends Store> extends AbstractNode {
	protected final static Logger logger = LoggerFactory.getLogger(AbstractLocalNode.class);

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
	 * Cached error messages of requests that failed their {@link #checkTransaction(TransactionRequest)}.
	 * This is useful to avoid polling for the outcome of recent requests whose
	 * {@link #checkTransaction(TransactionRequest)} failed, hence never
	 * got the chance to pass to {@link #deliverTransaction(TransactionRequest)}.
	 */
	private final LRUCache<TransactionReference, String> recentErrors;

	/**
	 * Cached recent requests that have had their signature checked.
	 * This avoids repeated signature checking in {@link #checkTransaction(TransactionRequest)}
	 * and {@link #deliverTransaction(TransactionRequest)}.
	 */
	private final LRUCache<SignedTransactionRequest, Boolean> checkedSignatures;

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
	 * The algorithm used for signing the requests for this node.
	 */
	private final SignatureAlgorithm<SignedTransactionRequest> signatureForRequests;

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
	 * The amount of gas allowed for the execution of the reward method of the validators
	 * at each committed block.
	 */
	private final static BigInteger GAS_FOR_REWARD = BigInteger.valueOf(100_000L);

	/**
	 * The amount of gas allowed for the execution of the method that increases the
	 * version of the verification module to use.
	 */
	private final static BigInteger GAS_FOR_INCREASE_VERIFICATION_VERSION = BigInteger.valueOf(10_000L);

	/**
	 * The amount of gas allowed for the execution of the method of the gas station
	 * that yields the gas price.
	 */
	private final static BigInteger GAS_FOR_GET_GAS_PRICE = BigInteger.valueOf(10_000L);

	/**
	 * The default gas model of the node.
	 */
	private final static GasCostModel defaultGasCostModel = new StandardGasCostModel();

	/**
	 * The reference to the contract that manages the validators of the node.
	 * After each transaction that consumes gas, this contract receives the
	 * price of the gas, that can later be redistributed to the validators.
	 */
	private volatile StorageReference validatorsCached;

	/**
	 * The reference to the object that manages the versions of the modules of the node.
	 */
	private volatile StorageReference versionsCached;

	/**
	 * The reference to the object that computes the cost of the gas.
	 */
	private volatile StorageReference gasStationCached;

	/**
	 * The gas consumed for CPU execution or storage since the last reward of the validators.
	 */
	private volatile BigInteger gasConsumedForCpuOrStorage;

	/**
	 * Builds the node.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractLocalNode(C config) {
		try {
			this.classLoadersCache = new LRUCache<>(100, 1000);
			this.gasConsumedForCpuOrStorage = ZERO;
			this.requestsCache = new LRUCache<>(100, config.requestCacheSize);
			this.responsesCache = new LRUCache<>(100, config.responseCacheSize);
			this.recentErrors = new LRUCache<>(100, 1000);
			this.checkedSignatures = new LRUCache<>(100, 1000);
			this.executor = Executors.newCachedThreadPool();
			this.config = config;
			this.hashingForRequests = hashingForRequests();
			this.semaphores = new ConcurrentHashMap<>();
			this.checkTime = new AtomicLong();
			this.deliverTime = new AtomicLong();
			this.closed = new AtomicBoolean();
			// we do not take into account the signature itself
			this.signatureForRequests = SignatureAlgorithm.mk(config.signature, SignedTransactionRequest::toByteArrayWithoutSignature);

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
	protected AbstractLocalNode(AbstractLocalNode<C,S> parent) {
		super(parent);

		this.classLoadersCache = parent.classLoadersCache;
		this.gasConsumedForCpuOrStorage = parent.gasConsumedForCpuOrStorage;
		this.requestsCache = parent.requestsCache;
		this.responsesCache = parent.responsesCache;
		this.recentErrors = parent.recentErrors;
		this.checkedSignatures = parent.checkedSignatures;
		this.executor = parent.executor;
		this.config = parent.config;
		this.store = mkStore();
		this.hashingForRequests = parent.hashingForRequests;
		this.semaphores = parent.semaphores;
		this.checkTime = parent.checkTime;
		this.deliverTime = parent.deliverTime;
		this.closed = parent.closed;
		this.signatureForRequests = parent.signatureForRequests;
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
		logger.info("invalidating caches");
		classLoadersCache.clear();
		requestsCache.clear();
		responsesCache.clear();
		recentErrors.clear();
		checkedSignatures.clear();
		validatorsCached = null;
		versionsCached = null;
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
	public final SignatureAlgorithm<SignedTransactionRequest> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		return signatureForRequests;
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
					TransactionResponse response = getResponse(reference);
					getRequest(reference);
					return response;
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

				// first we check if the request passed its checkTransaction
				// but failed its deliverTransaction: in that case, the node contains
				// the error message in its store
				Optional<String> error = store.getError(_reference);
				if (error.isPresent())
					throw new TransactionRejectedException(error.get());

				// then we check if the request did not pass its checkTransaction():
				// in that case, we might have its error message in cache
				String recentError = recentErrors.get(_reference);
				if (recentError != null)
					throw new TransactionRejectedException(recentError);

				// then we check if we have the response of the request in the store
				return store.getResponse(_reference)
					.orElseThrow(() -> new NoSuchElementException("unknown transaction reference " + _reference));
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
			logger.info(reference + ": checking start (" + request.getClass().getSimpleName() + ')');
			responseBuilderFor(reference, request);
			logger.info(reference + ": checking success");
		}
		catch (TransactionRejectedException e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			// we do not store the error message, since a failed checkTransaction
			// means that nobody is paying for this and we cannot expand the store;
			// we just take note of the failure to avoid polling for the response
			recentErrors.put(reference, trimmedMessage(e));
			logger.info(reference + ": checking failed: " + trimmedMessage(e));
			throw e;
		}
		catch (Exception e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			// we do not store the error message, since a failed checkTransaction
			// means that nobody is paying for this and we cannot expand the store;
			// we just take note of the failure to avoid polling for the response
			recentErrors.put(reference, trimmedMessage(e));
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
	 * @return the response; if this node has a notion of commit, this response is typically
	 *         still uncommitted
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	public final TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		TransactionReference reference = referenceOf(request);

		try {
			logger.info(reference + ": delivering start");
			recentErrors.put(reference, null);
			ResponseBuilder<?,?> responseBuilder = responseBuilderFor(reference, request);
			TransactionResponse response = responseBuilder.getResponse();
			int versionBeforePush = getVerificationVersion();
			store.push(reference, request, response);
			int versionAfterPush = getVerificationVersion();
			responseBuilder.pushReverification(store);
			if (versionBeforePush != versionAfterPush) {
				classLoadersCache.clear(); // force recomputation of classloaders after an update of the verification rules
				logger.info(reference + ": verification version set to " + versionAfterPush);
			}
			scheduleForNotificationOfEvents(response);
			takeNoteOfGas(request, response);
			logger.info(reference + ": delivering success");
			return response;
		}
		catch (TransactionRejectedException e) {
			store.push(reference, request, trimmedMessage(e));
			logger.info(reference + ": delivering failed: " + trimmedMessage(e));
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
	 * Rewards the validators with the cost of the gas consumed by the
	 * transactions in the last block. This is meaningful only if the
	 * node has some form of commit.
	 * 
	 * @param behaving the space-separated sequence of identifiers of the
	 *                 validators that behaved correctly during the creation
	 *                 of the last block
	 * @param misbehaving the space-separated sequence of the identifiers that
	 *                    misbehaved during the creation of the last block
	 * @return true if and only if rewarding was performed; rewarding might not be
	 *         performed because the manifest is not yet installed or because
	 *         the code of the validators contract failed
	 */
	public boolean rewardValidators(String behaving, String misbehaving) {
		try {
			Optional<StorageReference> manifest = store.getManifestUncommitted();
			if (manifest.isPresent()) {
				// we use the manifest as caller, since it is an externally-owned account
				StorageReference caller = manifest.get();
				BigInteger nonce = getNonceUncommitted(caller);
				StorageReference validators = getValidators();
				BigInteger balance = getBalance(validators);
				
				TransactionReference takamakaCode = getTakamakaCode();
				InstanceSystemMethodCallTransactionRequest request = new InstanceSystemMethodCallTransactionRequest
					(caller, nonce, GAS_FOR_REWARD, takamakaCode, CodeSignature.REWARD, validators, new StringValue(behaving), new StringValue(misbehaving), new BigIntegerValue(gasConsumedForCpuOrStorage));

				checkTransaction(request);
				TransactionResponse response = deliverTransaction(request);
				if (response instanceof MethodCallTransactionFailedResponse) {
					MethodCallTransactionFailedResponse responseAsFailed = (MethodCallTransactionFailedResponse) response;
					logger.error("could not reward the validators: " + responseAsFailed.where + ": " + responseAsFailed.classNameOfCause + ": " + responseAsFailed.messageOfCause);
				}
				else {
					BigInteger gasPrice;

					try {
						gasPrice = ((BigIntegerValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
							(caller, GAS_FOR_GET_GAS_PRICE, takamakaCode, CodeSignature.GET_GAS_PRICE, getGasStation()))).value;
					}
					catch (Exception e) {
						gasPrice = BigInteger.valueOf(-1L);
					}

					logger.info("units of coin rewarded to the validators for their work: " + balance);
					logger.info("units of gas consumed for CPU or storage since the previous reward: " + gasConsumedForCpuOrStorage);
					if (gasPrice.signum() >= 0)
						logger.info("the gas price is now " + gasPrice);
					else
						logger.info("the gas price could not be determined");

					gasConsumedForCpuOrStorage = ZERO;
					return true;
				}
			}
		}
		catch (Exception e) {
			logger.error("could not reward the validators", e);
		}

		return false;
	}

	public final void increaseVerificationVersion() { // TODO: remove at the end
		try {
			Optional<StorageReference> manifest = store.getManifestUncommitted();
			if (manifest.isPresent()) {
				// we use the manifest as caller, since it is an externally-owned account
				StorageReference caller = manifest.get();
				BigInteger nonce = getNonceUncommitted(caller);
				StorageReference versions = getVersions();
				InstanceSystemMethodCallTransactionRequest request = new InstanceSystemMethodCallTransactionRequest
					(caller, nonce, GAS_FOR_INCREASE_VERIFICATION_VERSION, getTakamakaCode(), CodeSignature.INCREASE_VERIFICATION_VERSION, versions);
	
				checkTransaction(request);
				deliverTransaction(request);
				logger.info("the version of the verification module has been set to " + getVerificationVersion());
			}
		}
		catch (Exception e) {
			logger.error("could not increase the version of the verification module to use for the next transactions", e);
		}
	}

	/**
	 * Yields the base cost of the given transaction. Normally, this is just
	 * {@code request.size(gasCostModel)}, but subclasses might redefine.
	 * 
	 * @param request the request of the transaction
	 * @param gasCostModel the gas cost model to use
	 * @return the base cost of the transaction
	 */
	public BigInteger getRequestStorageCost(NonInitialTransactionRequest<?> request, GasCostModel gasCostModel) {
		return request.size(gasCostModel);
	}

	/**
	 * Checks that the given request is signed with the private key of its caller.
	 * 
	 * @param request the request
	 * @param signatureAlgorithm the algorithm that must have been used for signing the request
	 * @return true if and only if the signature of {@code request} is valid
	 * @throws Exception if the signature of the request could not be checked
	 */
	protected final boolean signatureIsValid(SignedTransactionRequest request, SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm) throws Exception {
		return checkedSignatures.computeIfAbsent(request, _request -> signatureAlgorithm.verify(_request, getPublicKey(_request.getCaller(), signatureAlgorithm), _request.getSignature()));
	}

	/**
	 * Yields the chain identifier of this node, as reported in its manifest.
	 * 
	 * @return the chain identifier; if this node has not its chain identifier set yet, it yields the empty string
	 */
	protected final String getChainId() {
		Optional<StorageReference> manifest = getStore().getManifestUncommitted();
		if (manifest.isPresent())
			return ((UpdateOfString) getLastUpdateToFieldUncommitted(manifest.get(), FieldSignature.MANIFEST_CHAIN_ID_FIELD)).value;
		else
			// the manifest has not been set yet: requests can be executed if their chain identifier is the empty string
			return "";
	}

	/**
	 * Yields the reference to the contract that collects the validators of the node.
	 * After each transaction that consumes gas, the price of the gas is sent to this
	 * contract, that can later redistribute the reward to all validators.
	 * 
	 * @return the reference to the contract, inside the store of the node; if this node
	 *         has not its manifest set yet, it yields {@code null}
	 */
	protected final StorageReference getValidators() {
		if (validatorsCached == null)
			getStore().getManifestUncommitted().ifPresent
				(_manifest -> validatorsCached = ((UpdateOfStorage) getLastUpdateToFieldUncommitted(_manifest, FieldSignature.MANIFEST_VALIDATORS_FIELD)).value);

		return validatorsCached;
	}

	/**
	 * Yields the reference to the objects that keeps track of the
	 * versions of the module of the node.
	 * 
	 * @return the reference to the object, inside the store of the node; if this node
	 *         has not its manifest set yet, it yields {@code null}
	 */
	protected final StorageReference getVersions() {
		if (versionsCached == null)
			getStore().getManifestUncommitted().ifPresent
				(_manifest -> versionsCached = ((UpdateOfStorage) getLastUpdateToFieldUncommitted(_manifest, FieldSignature.MANIFEST_VERSIONS_FIELD)).value);

		return versionsCached;
	}

	/**
	 * Yields the current version of the verification module of the node.
	 * 
	 * @return the current version of the verification module
	 */
	protected final int getVerificationVersion() {
		StorageReference versions = getVersions();
		if (versions != null)
			return ((UpdateOfInt) getLastUpdateToFieldUncommitted(versions, FieldSignature.VERSIONS_VERIFICATION_VERSIONS_FIELD)).value;
		else
			// if the manifest is not available yet, the initial version of the module is installed, which is assumed to be 0
			return 0;
	}

	/**
	 * Yields the reference to the objects that keeps track of the gas cost.
	 * 
	 * @return the reference to the object, inside the store of the node; if this node
	 *         has not its manifest set yet, it yields {@code null}
	 */
	protected final StorageReference getGasStation() {
		if (gasStationCached == null)
			getStore().getManifestUncommitted().ifPresent
				(_manifest -> gasStationCached = ((UpdateOfStorage) getLastUpdateToFieldUncommitted(_manifest, FieldSignature.MANIFEST_GAS_STATION_FIELD)).value);

		return gasStationCached;
	}

	/**
	 * Yields the current gas price of the node.
	 * 
	 * @return the current gas price of the node
	 */
	protected final BigInteger getGasPrice() {
		StorageReference gasStation = getGasStation();
		if (gasStation != null)
			return ((UpdateOfBigInteger) getLastUpdateToFieldUncommitted(gasStation, FieldSignature.GAS_STATION_GAS_PRICE_FIELD)).value;
		else
			// if the manifest is not available yet, the initial gas price is 0
			return ZERO;
	}

	/**
	 * Determines if this node is initialized, that is, its manifest has been set,
	 * although possibly not yet committed.
	 * 
	 * @return true if and only if that condition holds
	 */
	protected final boolean isInitializedUncommitted() {
		return getStore().getManifestUncommitted().isPresent();
	}

	/**
	 * Yields the nonce of the given externally owned account.
	 * 
	 * @param account the account
	 * @return the nonce
	 */
	protected final BigInteger getNonceUncommitted(StorageReference account) {
		UpdateOfField updateOfNonce = getStore().getHistoryUncommitted(account)
			.map(transaction -> getLastUpdateOfNonceUncommitted(account, transaction))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst()
			.orElseThrow(() -> new DeserializationError("did not find the last update to the nonce of " + account));

		return ((BigIntegerValue) updateOfNonce.getValue()).value;
	}

	/**
	 * Yields the total balance of the given contract (green plus red, if any).
	 * 
	 * @param contract the contract
	 * @return the total balance
	 */
	protected final BigInteger getTotalBalance(StorageReference contract) {
		return getState(contract)
			.filter(update -> update instanceof UpdateOfBalance || update instanceof UpdateOfRedBalance)
			.map(update -> (UpdateOfField) update)
			.map(UpdateOfField::getValue)
			.map(value -> ((BigIntegerValue) value).value)
			.reduce(ZERO, BigInteger::add);
	}

	/**
	 * Yields the (green) balance of the given contract.
	 * 
	 * @param contract the contract
	 * @return the balance
	 */
	protected final BigInteger getBalance(StorageReference contract) {
		return ((BigIntegerValue) getLastUpdateToFieldUncommitted(contract, FieldSignature.BALANCE_FIELD).getValue()).value;
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
    	else if (request instanceof AbstractInstanceMethodCallTransactionRequest)
    		return new InstanceMethodCallResponseBuilder(reference, (AbstractInstanceMethodCallTransactionRequest) request, this);
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
	 * @throws TransactionRejectedException if the request was already present in the store
	 */
	protected final TransactionReference post(TransactionRequest<?> request) throws TransactionRejectedException {
		TransactionReference reference = referenceOf(request);
		logger.info(reference + ": posting (" + request.getClass().getSimpleName() + ')');

		if (store.getResponseUncommitted(reference).isPresent())
			throw new TransactionRejectedException("repeated request");

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
	 * Determines if the given initial transaction can still be run after the
	 * initialization of the node. Normally, this is false. However, specific
	 * implementations of the node might redefine and allow it.
	 * 
	 * @param request the request
	 * @return true if only if the execution of {@code request} is allowed
	 *         also after the initialization of this node
	 */
	protected boolean admitsAfterInitialization(InitialTransactionRequest<?> request) {
		return false;
	}

	/**
	 * Yields the reference to the translation that would be originated for the given request.
	 * 
	 * @param request the request
	 * @return the transaction reference
	 */
	protected final LocalTransactionReference referenceOf(TransactionRequest<?> request) {
		return new LocalTransactionReference(bytesToHex(hashingForRequests.hash(request)));
	}

	/**
	 * Yields the most recent update for the given field
	 * of the object with the given storage reference.
	 * If this node has some form of commit, the last update might
	 * not necessarily be already committed.
	 * 
	 * @param storageReference the storage reference
	 * @param field the field whose update is being looked for
	 * @return the update
	 */
	protected final UpdateOfField getLastUpdateToFieldUncommitted(StorageReference storageReference, FieldSignature field) {
		return getStore().getHistoryUncommitted(storageReference)
			.map(transaction -> getLastUpdateForUncommitted(storageReference, field, transaction))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst().orElseThrow(() -> new DeserializationError("did not find the last update for " + field + " of " + storageReference));
	}

	private void scheduleForNotificationOfEvents(TransactionResponse response) {
		if (response instanceof TransactionResponseWithEvents) {
			TransactionResponseWithEvents responseWithEvents = (TransactionResponseWithEvents) response;
			if (responseWithEvents.getEvents().count() > 0L)
				scheduleForNotificationOfEvents(responseWithEvents);
		}
	}

	private void takeNoteOfGas(TransactionRequest<?> request, TransactionResponse response) {
		if (response instanceof NonInitialTransactionResponse && !(request instanceof SystemTransactionRequest)) {
			NonInitialTransactionResponse responseAsNonInitial = (NonInitialTransactionResponse) response;
			gasConsumedForCpuOrStorage = gasConsumedForCpuOrStorage
				.add(responseAsNonInitial.gasConsumedForCPU)
				.add(responseAsNonInitial.gasConsumedForStorage);
		}
	}

	/**
	 * Schedules the events in the given response for notification to all their subscribers.
	 * This might call {@link #notifyEventsOf(TransactionResponseWithEvents)} immediately
	 * or might delay its call to next commit, if there is a notion of commit.
	 * In this way, one can guarantee that events are notified only when they have been committed.
	 * 
	 * @param response the response that contains events
	 */
	protected abstract void scheduleForNotificationOfEvents(TransactionResponseWithEvents response);

	/**
	 * Notifies all events contained in the given response.
	 * 
	 * @param response the response that contains the events
	 */
	protected final void notifyEventsOf(TransactionResponseWithEvents response) {
		response.getEvents().forEachOrdered(this::notifyEvent);
	}

	/**
	 * Extracts the key of the given event and notifies the event to all event handlers for that key.
	 * 
	 * @param event the event to notify
	 */
	private void notifyEvent(StorageReference event) {
		// we extract the key from the event; since it is a final field of the event,
		// it must be defined by the transaction that created the event; the event might
		// have been created in a transaction that is not yet committed
		try {
			TransactionResponse response = store.getResponseUncommitted(event.transaction).get();
			if (!(response instanceof TransactionResponseWithUpdates))
				throw new NoSuchElementException("transaction reference " + event.transaction + " does not contain updates");
	
			StorageReference creator = ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfStorage && update.object.equals(event))
				.map(update -> (UpdateOfStorage) update)
				.filter(update -> update.field.equals(FieldSignature.EVENT_CREATOR_FIELD))
				.findFirst().get().value;
	
			notifyEvent(creator, event);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Yields the public key of the given externally owned account.
	 * 
	 * @param reference the account
	 * @param signatureAlgorithm the signing algorithm used for the request
	 * @return the public key
	 * @throws NoSuchAlgorithmException if the signing algorithm is unknown
	 * @throws NoSuchProviderException of the signing provider is unknown
	 * @throws InvalidKeySpecException of the key specification is invalid
	 */
	private PublicKey getPublicKey(StorageReference reference, SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
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
	
		String publicKeyEncodedBase64 = ((TransactionResponseWithUpdates) response).getUpdates()
			.filter(update -> update instanceof UpdateOfString && update.object.equals(reference))
			.map(update -> (UpdateOfString) update)
			.filter(update -> update.getField().equals(FieldSignature.EOA_PUBLIC_KEY_FIELD) || update.getField().equals(FieldSignature.RGEOA_PUBLIC_KEY_FIELD))
			.findFirst().get()
			.value;
	
		byte[] publicKeyEncoded = Base64.getDecoder().decode(publicKeyEncodedBase64);
		return signatureAlgorithm.publicKeyFromEncoded(publicKeyEncoded);
	}

	private void checkTransactionReference(TransactionReference reference) {
		// each byte is represented by two successive characters
		String hash;

		if (reference == null || (hash = reference.getHash()) == null || hash.length() != hashingForRequests.length() * 2)
			throw new IllegalArgumentException("illegal transaction reference " + reference + ": it should hold a hash of " + hashingForRequests.length() * 2 + " characters");

		if (hash.chars().map(HEX_CHARS::indexOf).anyMatch(index -> index == -1))
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
	    int pos = 0;
	    for (byte b: bytes) {
	        int v = b & 0xFF;
	        hexChars[pos++] = HEX_ARRAY[v >>> 4];
	        hexChars[pos++] = HEX_ARRAY[v & 0x0F];
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
		Set<Field> allFields = collectAllFieldsOf(classTag.get().className, classLoader);
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
	 * Yields the update to the given field of the object at the given reference,
	 * generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param transaction the reference to the transaction
	 * @return the update, if any. If the field of {@code object} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdateForUncommitted(StorageReference object, FieldSignature field, TransactionReference transaction) {
		TransactionResponse response = getStore().getResponseUncommitted(transaction)
			.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + transaction));

		if (response instanceof TransactionResponseWithUpdates)
			return ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(object) && update.getField().equals(field))
				.findFirst();
	
		return Optional.empty();
	}

	/**
	 * Yields the update to the nonce of the given account, generated during a given transaction.
	 * 
	 * @param account the reference of the account
	 * @param transaction the reference to the transaction
	 * @return the update to the nonce, if any. If the nonce of {@code account} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdateOfNonceUncommitted(StorageReference account, TransactionReference transaction) {
		TransactionResponse response = getStore().getResponseUncommitted(transaction)
			.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + transaction));

		if (response instanceof TransactionResponseWithUpdates)
			return ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(account) && (update.getField().equals(FieldSignature.EOA_NONCE_FIELD) || update.getField().equals(FieldSignature.RGEOA_NONCE_FIELD)))
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
	 * @return the fields
	 */
	private static Set<Field> collectAllFieldsOf(String className, EngineClassLoader classLoader) {
		Set<Field> bag = new HashSet<>();
		Class<?> storage = classLoader.getStorage();

		try {
			for (Class<?> clazz = classLoader.loadClass(className), previous = null; previous != storage; previous = clazz, clazz = clazz.getSuperclass())
				Stream.of(clazz.getDeclaredFields())
					.filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
					.forEach(bag::add);
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}

		return bag;
	}
}