package io.takamaka.code.engine;

import static java.math.BigInteger.ZERO;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.AbstractNode;
import io.hotmoka.nodes.ConsensusParams;
import io.takamaka.code.engine.internal.NodeCachesImpl;
import io.takamaka.code.engine.internal.StoreUtilitiesImpl;
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
	 * An object that provides utility methods on {@link #store}.
	 */
	protected final StoreUtilities storeUtilities;

	/**
	 * The caches of the node.
	 */
	protected final NodeCaches caches;

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
	 * The amount of gas allowed for the execution of the reward method of the validators
	 * at each committed block.
	 */
	private final static BigInteger GAS_FOR_REWARD = BigInteger.valueOf(100_000L);

	/**
	 * The default gas model of the node.
	 */
	private final static GasCostModel defaultGasCostModel = new StandardGasCostModel();

	/**
	 * The gas consumed for CPU execution or storage since the last reward of the validators.
	 */
	private volatile BigInteger gasConsumedForCpuOrStorage;

	/**
	 * Builds a node with a brand new, empty store.
	 * 
	 * @param config the configuration of the node
	 * @param consensus the consensus parameters at the beginning of the life of the node
	 */
	protected AbstractLocalNode(C config, ConsensusParams consensus) {
		this(config, consensus, true);
	}

	/**
	 * Builds a node, recycling a previous existing store. The store must be that
	 * of an already initialized node, whose consensus parameters are recovered from its manifest.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractLocalNode(C config) {
		this(config, null, false);
	}

	private AbstractLocalNode(C config, ConsensusParams consensus, boolean deleteDir) {
		try {
			this.config = config;
			this.storeUtilities = new StoreUtilitiesImpl(this);
			this.caches = new NodeCachesImpl(this, consensus, this::checkTransactionReference, storeUtilities);
			this.gasConsumedForCpuOrStorage = ZERO;
			this.executor = Executors.newCachedThreadPool();
			this.hashingForRequests = HashingAlgorithm.sha256(Marshallable::toByteArray);
			this.semaphores = new ConcurrentHashMap<>();
			this.checkTime = new AtomicLong();
			this.deliverTime = new AtomicLong();
			this.closed = new AtomicBoolean();

			if (deleteDir) {
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

		this.caches = parent.caches;
		this.gasConsumedForCpuOrStorage = parent.gasConsumedForCpuOrStorage;
		this.executor = parent.executor;
		this.config = parent.config;
		this.store = mkStore();
		this.storeUtilities = parent.storeUtilities;
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
	 */
	@Override
	public final SignatureAlgorithm<SignedTransactionRequest> getSignatureAlgorithmForRequests() {
		return caches.getConsensusParams().getSignature();
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
			return caches.getRequest(reference);
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
			return caches.getResponse(reference);
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
			if (!isCommitted(reference.transaction))
				throw new NoSuchElementException("unknown transaction reference " + reference.transaction);

			return storeUtilities.getClassTagUncommitted(reference);
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
			if (!isCommitted(reference.transaction))
				throw new NoSuchElementException("unknown transaction reference " + reference.transaction);

			return storeUtilities.getStateCommitted(reference);
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
			logger.info(reference + ": running start (" + request.getClass().getSimpleName() + " -> " + request.method.methodName + ')');
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
			caches.recentCheckTransactionError(reference, trimmedMessage(e));
			logger.info(reference + ": checking failed: " + trimmedMessage(e));
			throw e;
		}
		catch (Exception e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			// we do not store the error message, since a failed checkTransaction
			// means that nobody is paying for this and we cannot expand the store;
			// we just take note of the failure to avoid polling for the response
			caches.recentCheckTransactionError(reference, trimmedMessage(e));
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
			caches.recentCheckTransactionError(reference, null);
			ResponseBuilder<?,?> responseBuilder = responseBuilderFor(reference, request);
			TransactionResponse response = responseBuilder.getResponse();
			store.push(reference, request, response);
			responseBuilder.replaceReverifiedResponses();
			scheduleForNotificationOfEvents(response);
			takeNoteOfGas(request, response);
			invalidateCachesIfNeeded(response, responseBuilder.getClassLoader());
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
	public final boolean rewardValidators(String behaving, String misbehaving) {
		// the node might not have completed its initialization yet
		if (caches.getConsensusParams() == null)
			return false;

		try {
			Optional<StorageReference> manifest = store.getManifestUncommitted();
			if (manifest.isPresent()) {
				// we use the manifest as caller, since it is an externally-owned account
				StorageReference caller = manifest.get();
				BigInteger nonce = storeUtilities.getNonceUncommitted(caller);
				StorageReference validators = caches.getValidators().get(); // ok, since the manifest is present
				BigInteger balance = storeUtilities.getBalanceUncommitted(validators);
				
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
					logger.info("units of coin rewarded to the validators for their work: " + balance);
					logger.info("units of gas consumed for CPU or storage since the previous reward: " + gasConsumedForCpuOrStorage);
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

	/**
	 * Clears the caches of this node.
	 */
	protected void invalidateCaches() {
		caches.invalidate();
		gasConsumedForCpuOrStorage = ZERO;
		logger.info("the caches of the node have been invalidated");
	}

	/**
	 * Invalidates the caches, if needed, after the addition of the given response into store.
	 * 
	 * @param response the store
	 * @param classLoader the class loader of the transaction that computed {@code response}
	 */
	protected void invalidateCachesIfNeeded(TransactionResponse response, EngineClassLoader classLoader) {
		caches.invalidateIfNeeded(response, classLoader);
	}

	/**
	 * Yields the base cost of the given transaction. Normally, this is just
	 * {@code request.size(gasCostModel)}, but subclasses might redefine.
	 * 
	 * @param request the request of the transaction
	 * @param gasCostModel the gas cost model to use
	 * @return the base cost of the transaction
	 */
	protected BigInteger getRequestStorageCost(NonInitialTransactionRequest<?> request, GasCostModel gasCostModel) {
		return request.size(gasCostModel);
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
	 * Yields the error message trimmed to a maximal length, to avoid overflow.
	 *
	 * @param t the throwable whose error message is processed
	 * @return the resulting message
	 */
	protected String trimmedMessage(Throwable t) {
		String message = t.getMessage();
		int length = message.length();
	
		int maxErrorLength = caches.getConsensusParams().maxErrorLength;
	
		if (length > maxErrorLength)
			return message.substring(0, maxErrorLength) + "...";
		else
			return message;
	}

	/**
	 * Schedules the events in the given response for notification to all their subscribers.
	 * This might call {@link #notifyEventsOf(TransactionResponseWithEvents)} immediately
	 * or might delay its call to the next commit, if there is a notion of commit.
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
		try {
			response.getEvents().forEachOrdered(event -> notifyEvent(storeUtilities.getCreatorUncommitted(event), event));
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}	
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
	 * Determines if the given transaction has been committed already.
	 * 
	 * @param transaction the transaction
	 * @return true if and only if that condition holds
	 */
	private boolean isCommitted(TransactionReference transaction) {
		try {
			getResponse(transaction);
			return true;
		}
		catch (TransactionRejectedException | NoSuchElementException e) {
			return false;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	private void checkTransactionReference(TransactionReference reference) {
		// each byte is represented by two successive characters
		String hash;
	
		if (reference == null || (hash = reference.getHash()) == null || hash.length() != hashingForRequests.length() * 2)
			throw new IllegalArgumentException("illegal transaction reference " + reference + ": it should hold a hash of " + hashingForRequests.length() * 2 + " characters");
	
		if (hash.chars().map(HEX_CHARS::indexOf).anyMatch(index -> index == -1))
			throw new IllegalArgumentException("illegal transaction reference " + reference + ": only \"" + HEX_CHARS + "\" are allowed");
	}

	private void takeNoteOfGas(TransactionRequest<?> request, TransactionResponse response) {
		if (response instanceof NonInitialTransactionResponse && !(request instanceof SystemTransactionRequest)) {
			NonInitialTransactionResponse responseAsNonInitial = (NonInitialTransactionResponse) response;
			gasConsumedForCpuOrStorage = gasConsumedForCpuOrStorage
				.add(responseAsNonInitial.gasConsumedForCPU)
				.add(responseAsNonInitial.gasConsumedForStorage);
		}
	}

	private void scheduleForNotificationOfEvents(TransactionResponse response) {
		if (response instanceof TransactionResponseWithEvents) {
			TransactionResponseWithEvents responseWithEvents = (TransactionResponseWithEvents) response;
			if (responseWithEvents.getEvents().count() > 0L)
				scheduleForNotificationOfEvents(responseWithEvents);
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
}