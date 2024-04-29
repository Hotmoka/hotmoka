/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.local.internal;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
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
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.AbstractAutoCloseableWithLockAndOnCloseHandlers;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.instrumentation.GasCostModels;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.CodeFutures;
import io.hotmoka.node.JarFutures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.SubscriptionsManagers;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.UninitializedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConstructorFuture;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.MethodFuture;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.SubscriptionsManager;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.AbstractInstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.NonInitialTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SystemTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.FailedTransactionResponse;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.NodeCache;
import io.hotmoka.node.local.api.ResponseBuilder;
import io.hotmoka.node.local.api.StoreUtility;
import io.hotmoka.node.local.internal.transactions.ConstructorCallResponseBuilder;
import io.hotmoka.node.local.internal.transactions.GameteCreationResponseBuilder;
import io.hotmoka.node.local.internal.transactions.InitializationResponseBuilder;
import io.hotmoka.node.local.internal.transactions.InstanceMethodCallResponseBuilder;
import io.hotmoka.node.local.internal.transactions.InstanceViewMethodCallResponseBuilder;
import io.hotmoka.node.local.internal.transactions.JarStoreInitialResponseBuilder;
import io.hotmoka.node.local.internal.transactions.JarStoreResponseBuilder;
import io.hotmoka.node.local.internal.transactions.StaticMethodCallResponseBuilder;
import io.hotmoka.node.local.internal.transactions.StaticViewMethodCallResponseBuilder;
import io.hotmoka.stores.AbstractStore;
import io.hotmoka.stores.Store;
import io.hotmoka.stores.StoreException;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * 
 * @param <C> the type of the configuration object used by the node
 * @param <S> the type of the store of the node
 */
@ThreadSafe
public abstract class AbstractLocalNodeImpl<C extends LocalNodeConfig<?,?>, S extends AbstractStore<S>> extends AbstractAutoCloseableWithLockAndOnCloseHandlers<ClosedNodeException> implements Node {
	/**
	 * The version of Hotmoka used by the nodes.
	 */
	public final static String HOTMOKA_VERSION;

	static {
		// we access the Maven properties from the pom.xml file of the project
		try (InputStream is = AbstractLocalNodeImpl.class.getModule().getResourceAsStream("io.hotmoka.node.local.maven.properties")) {
			Objects.requireNonNull(is, "Cannot find io.hotmoka.node.local.maven.properties");
			var mavenProperties = new Properties();
			mavenProperties.load(is);
			HOTMOKA_VERSION = mavenProperties.getProperty("hotmoka.version");
		}
		catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final static Logger LOGGER = Logger.getLogger(AbstractLocalNodeImpl.class.getName());

	/**
	 * The manager of the subscriptions to the events occurring in this node.
	 */
	private final SubscriptionsManager subscriptions = SubscriptionsManagers.mk();

	@Override
	public final Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler) {
		return subscriptions.subscribeToEvents(creator, handler);
	}

	/**
	 * Notifies the given event to all event handlers for the given creator.
	 * 
	 * @param creator the creator of the event
	 * @param event the event to notify
	 */
	protected final void notifyEvent(StorageReference creator, StorageReference event) {
		subscriptions.notifyEvent(creator, event);
		LOGGER.info(event + ": notified as event with creator " + creator);
	}

	/**
	 * The configuration of the node.
	 */
	protected final C config;

	/**
	 * The hasher for transaction requests.
	 */
	private final Hasher<TransactionRequest<?>> hasher;

	/**
	 * An object that provides utility methods on {@link #store}.
	 */
	protected final StoreUtility storeUtilities;

	/**
	 * The caches of the node.
	 */
	protected final NodeCache caches;

	/**
	 * The store of the node.
	 */
	protected S store;

	/**
	 * The gas model of the node.
	 */
	private final GasCostModel gasCostModel = GasCostModels.standard();

	/**
	 * A map that provides a semaphore for each currently executing transaction.
	 * It is used to block threads waiting for the outcome of transactions.
	 */
	private final ConcurrentMap<TransactionReference, Semaphore> semaphores;

	/**
	 * An executor for short background tasks.
	 */
	private final ExecutorService executors;

	/**
	 * The time spent for checking requests.
	 */
	private final AtomicLong checkTime;

	/**
	 * The time spent for delivering transactions.
	 */
	private final AtomicLong deliverTime;

	/**
	 * Cached error messages of requests that failed their {@link AbstractLocalNodeImpl#checkTransaction(TransactionRequest)}.
	 * This is useful to avoid polling for the outcome of recent requests whose
	 * {@link #checkTransaction(TransactionRequest)} failed, hence never
	 * got the chance to pass to {@link #deliverTransaction(TransactionRequest)}.
	 */
	private final LRUCache<TransactionReference, String> recentCheckTransactionErrors;

	/**
	 * True if this blockchain has been already closed. Used to avoid double-closing in the shutdown hook.
	 */
	private final AtomicBoolean closed;

	/**
	 * The gas consumed for CPU execution, RAM or storage since the last reward of the validators.
	 */
	private volatile BigInteger gasConsumedSinceLastReward;

	/**
	 * The reward to send to the validators at the next reward.
	 */
	private volatile BigInteger coinsSinceLastReward;

	/**
	 * The reward to send to the validators at the next reward, without considering the inflation.
	 */
	private volatile BigInteger coinsSinceLastRewardWithoutInflation;

	/**
	 * The number of transactions executed since the last reward.
	 */
	private volatile BigInteger numberOfTransactionsSinceLastReward;

	/**
	 * The view of this node with methods used by the implementation of this module.
	 */
	final NodeInternal internal = new NodeInternalImpl();

	/**
	 * The amount of gas allowed for the execution of the reward method of the validators
	 * at each committed block.
	 */
	private final static BigInteger GAS_FOR_REWARD = BigInteger.valueOf(100_000L);

	private final static BigInteger _100_000_000 = BigInteger.valueOf(100_000_000L);

	/**
	 * Builds a node with a brand new, empty store.
	 * 
	 * @param config the configuration of the node
	 * @param consensus the consensus parameters at the beginning of the life of the node
	 */
	protected AbstractLocalNodeImpl(C config, ConsensusConfig<?,?> consensus) {
		this(config, consensus, true);
	}

	/**
	 * Builds a node, recycling a previous existing store. The store must be that
	 * of an already initialized node, whose consensus parameters are recovered from its manifest.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractLocalNodeImpl(C config) {
		this(config, null, false);
	}

	private AbstractLocalNodeImpl(C config, ConsensusConfig<?,?> consensus, boolean deleteDir) {
		super(ClosedNodeException::new);

		this.config = config;

		try {
			this.hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception", e);
		}

		this.storeUtilities = new StoreUtilityImpl(internal);
		this.caches = new NodeCachesImpl(internal, consensus);
		this.recentCheckTransactionErrors = new LRUCache<>(100, 1000);
		this.gasConsumedSinceLastReward = ZERO;
		this.coinsSinceLastReward = ZERO;
		this.coinsSinceLastRewardWithoutInflation = ZERO;
		this.numberOfTransactionsSinceLastReward = ZERO;
		this.executors = Executors.newCachedThreadPool();
		this.semaphores = new ConcurrentHashMap<>();
		this.checkTime = new AtomicLong();
		this.deliverTime = new AtomicLong();
		this.closed = new AtomicBoolean();

		if (deleteDir) {
			try {
				deleteRecursively(config.getDir());  // cleans the directory where the node's data live
				Files.createDirectories(config.getDir());
			}
			catch (IOException e) {
			}
		}

		this.store = mkStore();
		addShutdownHook();
	}

	/**
	 * Factory method for creating the store of this node.
	 * 
	 * @return the store
	 */
	protected abstract S mkStore();

	/**
	 * Yields the time to use for the currently executing transaction.
	 * 
	 * @return the time
	 */
	protected abstract long getNow();

	/**
	 * Determines if this node has not been closed yet.
	 * This thread-safe method can be called to avoid double-closing of a node.
	 * 
	 * @return true if and only if the node has not been closed yet
	 */
	protected final boolean isNotYetClosed() {
		return !closed.getAndSet(true);
	}

	@Override
	public final void close() throws InterruptedException, NodeException {
		if (stopNewCalls())
			closeResources();
	}

	protected void closeResources() throws NodeException, InterruptedException {
		try {
			executors.shutdownNow();
		}
		finally {
			try {
				S store = this.store;
				if (store != null)
					store.close();
			}
			finally {
				// we give five seconds
				executors.awaitTermination(5, TimeUnit.SECONDS);
				LOGGER.info("time spent checking requests: " + checkTime + "ms");
				LOGGER.info("time spent delivering requests: " + deliverTime + "ms");
			}
		}
	}

	@Override
	public final ConsensusConfig<?,?> getConfig() throws NodeException {
		try (var scope = mkScope()) {
			return caches.getConsensusParams();
		}
	}

	@Override
	public final TransactionReference getTakamakaCode() throws NodeException {
		try (var scope = mkScope()) {
			var manifest = getManifest();

			try {
				return getClassTag(manifest).getJar();
			}
			catch (UnknownReferenceException e) {
				throw new NodeException("The manifest of the node cannot be found in the node itself", e);
			}
		}
	}

	@Override
	public final StorageReference getManifest() throws NodeException {
		try (var scope = mkScope()) {
			return store.getManifest().orElseThrow(UninitializedNodeException::new);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public final TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		try (var scope = mkScope()) {
			Objects.requireNonNull(reference);
			Semaphore semaphore = semaphores.get(reference);
			if (semaphore != null)
				semaphore.acquire();
	
			for (long attempt = 1, delay = config.getPollingDelay(); attempt <= Math.max(1L, config.getMaxPollingAttempts()); attempt++, delay = delay * 110 / 100)
				try {
					// we enforce that both request and response are available
					TransactionResponse response = getResponse(reference);
					getRequest(reference);
					return response;
				}
				catch (UnknownReferenceException e) {
					Thread.sleep(delay);
				}

			throw new TimeoutException("Cannot find the response of transaction reference " + reference + ": tried " + config.getMaxPollingAttempts() + " times");
		}
		catch (RuntimeException e) {
			LOGGER.log(Level.WARNING, "Unexpected exception", e);
			throw e;
		}
	}

	@Override
	public final TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, NodeException {
		try (var scope = mkScope()) {
			Objects.requireNonNull(reference);

			try {
				return caches.getRequest(reference).orElseThrow(() -> new UnknownReferenceException(reference));
			}
			catch (RuntimeException e) {
				LOGGER.log(Level.WARNING, "unexpected exception", e);
				throw e;
			}
		}
	}

	@Override
	public final TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, UnknownReferenceException, NodeException {
		try (var scope = mkScope()) {
			Objects.requireNonNull(reference);

			try {
				Optional<TransactionResponse> response = caches.getResponse(reference);
				if (response.isPresent())
					return response.get();

				// we check if the request passed its checkTransaction but failed its deliverTransaction:
				// in that case, the node contains the error message in its store; afterwards
				// we check if the request did not pass its checkTransaction():
				// in that case, we might have its error message in {@link #recentCheckTransactionErrors}
				String error = store.getError(reference).orElseGet(() -> recentCheckTransactionErrors.get(reference));

				if (error != null)
					throw new TransactionRejectedException(error);
				else
					throw new UnknownReferenceException(reference);
			}
			catch (RuntimeException e) {
				LOGGER.log(Level.WARNING, "Unexpected exception", e);
				throw e;
			}
			catch (StoreException e) {
				throw new NodeException(e);
			}
		}
	}

	@Override
	public final ClassTag getClassTag(StorageReference reference) throws UnknownReferenceException, NodeException {
		try (var scope = mkScope()) {
			Objects.requireNonNull(reference);

			if (isNotCommitted(reference.getTransaction()))
				throw new UnknownReferenceException(reference);

			return storeUtilities.getClassTagUncommitted(reference);
		}
		catch (NoSuchElementException e) {
			throw new UnknownReferenceException(reference);
		}
	}

	@Override
	public final Stream<Update> getState(StorageReference reference) throws UnknownReferenceException, NodeException {
		try (var scope = mkScope()) {
			Objects.requireNonNull(reference);
			try {
				if (isNotCommitted(reference.getTransaction()))
					throw new UnknownReferenceException(reference);

				return storeUtilities.getStateCommitted(reference);
			}
			catch (NoSuchElementException e) {
				throw new UnknownReferenceException(reference);
			}
			catch (StoreException e) {
				throw new NodeException(e);
			}
			catch (RuntimeException e) {
				LOGGER.log(Level.WARNING, "Unexpected exception", e);
				throw e;
			}
		}
	}

	@Override
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> {
			TransactionReference reference = post(request);
			getPolledResponse(reference);
			return reference;
		});
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		wrapInCaseOfExceptionSimple(() -> getPolledResponse(post(request))); // result unused
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> ((GameteCreationTransactionResponse) getPolledResponse(post(request))).getGamete());
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
	public final Optional<StorageValue> addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> postInstanceMethodCallTransaction(request).get());
	}

	@Override
	public final Optional<StorageValue> addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> postStaticMethodCallTransaction(request).get());
	}

	@Override
	public final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> {
			var reference = TransactionReferences.of(hasher.hash(request));
			LOGGER.info(reference + ": running start (" + request.getClass().getSimpleName() + " -> " + request.getStaticTarget().getMethodName() + ')');

			Optional<StorageValue> result;

			synchronized (deliverTransactionLock) {
				result = getOutcome(new InstanceViewMethodCallResponseBuilder(reference, request, internal).getResponse());
			}

			LOGGER.info(reference + ": running success");
			return result;
		});
	}

	@Override
	public final Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return wrapInCaseOfExceptionFull(() -> {
			var reference = TransactionReferences.of(hasher.hash(request));
			LOGGER.info(reference + ": running start (" + request.getClass().getSimpleName() + " -> " + request.getStaticTarget().getMethodName() + ')');
			Optional<StorageValue> result;

			synchronized (deliverTransactionLock) {
				result = getOutcome(new StaticViewMethodCallResponseBuilder(reference, request, internal).getResponse());
			}

			LOGGER.info(reference + ": running success");
			return result;
		});
	}

	@Override
	public final JarFuture postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> JarFutures.of(post(request), this));
	}

	@Override
	public final ConstructorFuture postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> CodeFutures.ofConstructor(post(request), this));
	}

	@Override
	public final MethodFuture postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> CodeFutures.ofMethod(post(request), this));
	}

	@Override
	public final MethodFuture postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return wrapInCaseOfExceptionSimple(() -> CodeFutures.ofMethod(post(request), this));
	}

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 */
	protected final void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		var reference = TransactionReferences.of(hasher.hash(request));
		recentCheckTransactionErrors.put(reference, null);

		try {
			LOGGER.info(reference + ": checking start (" + request.getClass().getSimpleName() + ')');
			responseBuilderFor(reference, request);
			LOGGER.info(reference + ": checking success");
		}
		catch (TransactionRejectedException e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			// we do not store the error message, since a failed checkTransaction
			// means that nobody is paying for this and we cannot expand the store;
			// we just take note of the failure to avoid polling for the response
			recentCheckTransactionErrors.put(reference, trimmedMessage(e));
			LOGGER.info(reference + ": checking failed: " + trimmedMessage(e));
			LOGGER.log(Level.INFO, "transaction rejected", e);
			throw e;
		}
		catch (RuntimeException e) {
			// we wake up who was waiting for the outcome of the request
			signalSemaphore(reference);
			// we do not store the error message, since a failed checkTransaction
			// means that nobody is paying for this and we cannot expand the store;
			// we just take note of the failure to avoid polling for the response
			recentCheckTransactionErrors.put(reference, trimmedMessage(e));
			LOGGER.log(Level.WARNING, reference + ": checking failed with unexpected exception", e);
			throw e;
		}
		finally {
			checkTime.addAndGet(System.currentTimeMillis() - start);
		}
	}

	/**
	 * A lock for the {@link #deliverTransaction(TransactionRequest)} body.
	 */
	private final Object deliverTransactionLock = new Object();

	/**
	 * Builds a response for the given request and adds it to the store of the node.
	 * 
	 * @param request the request
	 * @return the response; if this node has a notion of commit, this response is typically still uncommitted
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	protected final TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		long start = System.currentTimeMillis();

		var reference = TransactionReferences.of(hasher.hash(request));

		try {
			try {
				LOGGER.info(reference + ": delivering start (" + request.getClass().getSimpleName() + ')');

				TransactionResponse response;

				synchronized (deliverTransactionLock) {
					ResponseBuilder<?,?> responseBuilder = responseBuilderFor(reference, request);
					response = responseBuilder.getResponse();
					store = store.push(reference, request, response);
					responseBuilder.replaceReverifiedResponses();
					scheduleForNotificationOfEvents(response);
					takeNoteForNextReward(request, response);
					invalidateCachesIfNeeded(response, responseBuilder.getClassLoader());
				}

				LOGGER.info(reference + ": delivering success");
				return response;
			}
			catch (TransactionRejectedException e) {
				store.push(reference, request, trimmedMessage(e));
				LOGGER.info(reference + ": delivering failed: " + trimmedMessage(e));
				LOGGER.log(Level.INFO, "transaction rejected", e);
				throw e;
			}
			catch (ClassNotFoundException | NodeException | UnknownReferenceException e) {
				store.push(reference, request, trimmedMessage(e));
				LOGGER.log(Level.SEVERE, reference + ": delivering failed with unexpected exception", e);
				throw new RuntimeException(e);
			}
			catch (RuntimeException e) {
				store.push(reference, request, trimmedMessage(e));
				LOGGER.log(Level.WARNING, reference + ": delivering failed with unexpected exception", e);
				throw e;
			}
		}
		catch (StoreException e) {
			throw new RuntimeException(e); // TODO
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
	protected final boolean rewardValidators(String behaving, String misbehaving) {
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
				TransactionReference takamakaCode = getTakamakaCode();

				// we determine how many coins have been minted during the last reward:
				// it is the price of the gas distributed minus the same price without inflation
				BigInteger minted = coinsSinceLastReward.subtract(coinsSinceLastRewardWithoutInflation);

				// it might happen that the last distribution goes beyond the limit imposed
				// as final supply: in that case we truncate the minted coins so that the current
				// supply reaches the final supply, exactly; this might occur from below (positive inflation)
				// or from above (negative inflation)
				BigInteger currentSupply = storeUtilities.getCurrentSupplyUncommitted(validators);
				if (minted.signum() > 0) {
					BigInteger finalSupply = caches.getConsensusParams().getFinalSupply();
					BigInteger extra = finalSupply.subtract(currentSupply.add(minted));
					if (extra.signum() < 0)
						minted = minted.add(extra);
				}
				else if (minted.signum() < 0) {
					BigInteger finalSupply = caches.getConsensusParams().getFinalSupply();
					BigInteger extra = finalSupply.subtract(currentSupply.add(minted));
					if (extra.signum() > 0)
						minted = minted.add(extra);
				}

				InstanceSystemMethodCallTransactionRequest request = TransactionRequests.instanceSystemMethodCall
					(caller, nonce, GAS_FOR_REWARD, takamakaCode, MethodSignatures.VALIDATORS_REWARD, validators,
					StorageValues.bigIntegerOf(coinsSinceLastReward), StorageValues.bigIntegerOf(minted),
					StorageValues.stringOf(behaving), StorageValues.stringOf(misbehaving),
					StorageValues.bigIntegerOf(gasConsumedSinceLastReward), StorageValues.bigIntegerOf(numberOfTransactionsSinceLastReward));

				checkTransaction(request);
				ResponseBuilder<?,?> responseBuilder = responseBuilderFor(TransactionReferences.of(hasher.hash(request)), request);
				TransactionResponse response = responseBuilder.getResponse();
				// if there is only one update, it is the update of the nonce of the manifest: we prefer not to expand
				// the store with the transaction, so that the state stabilizes, which might give
				// to the node the chance of suspending the generation of new blocks
				if (!(response instanceof TransactionResponseWithUpdates) || ((TransactionResponseWithUpdates) response).getUpdates().count() > 1L)
					response = deliverTransaction(request);

				if (response instanceof MethodCallTransactionFailedResponse) {
					MethodCallTransactionFailedResponse responseAsFailed = (MethodCallTransactionFailedResponse) response;
					LOGGER.log(Level.WARNING, "could not reward the validators: " + responseAsFailed.getWhere() + ": " + responseAsFailed.getClassNameOfCause() + ": " + responseAsFailed.getMessageOfCause());
				}
				else {
					LOGGER.info("units of gas consumed for CPU, RAM or storage since the previous reward: " + gasConsumedSinceLastReward);
					LOGGER.info("units of coin rewarded to the validators for their work since the previous reward: " + coinsSinceLastReward);
					LOGGER.info("units of coin minted since the previous reward: " + minted);
					gasConsumedSinceLastReward = ZERO;
					coinsSinceLastReward = ZERO;
					coinsSinceLastRewardWithoutInflation = ZERO;
					numberOfTransactionsSinceLastReward = ZERO;

					return true;
				}
			}
		}
		catch (Exception e) {
			LOGGER.log(Level.WARNING, "could not reward the validators", e);
		}

		return false;
	}

	/**
	 * Yields the error message trimmed to a maximal length, to avoid overflow.
	 *
	 * @param t the throwable whose error message is processed
	 * @return the resulting message
	 */
	protected final String trimmedMessage(Throwable t) {
		String message = t.getMessage();
		int length = message.length();
	
		long maxErrorLength = caches.getConsensusParams().getMaxErrorLength();
	
		if (length > maxErrorLength)
			return message.substring(0, (int) maxErrorLength) + "...";
		else
			return message;
	}

	/**
	 * Notifies all events contained in the given response.
	 * 
	 * @param response the response that contains the events
	 */
	protected final void notifyEventsOf(TransactionResponseWithEvents response) {
		try {
			response.getEvents().forEachOrdered(event -> notifyEvent(storeUtilities.getCreatorUncommitted(event), event));
		}
		catch (RuntimeException e) {
			LOGGER.log(Level.WARNING, "unexpected exception", e);
			throw e;
		}	
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
		var reference = TransactionReferences.of(hasher.hash(request));
		LOGGER.info(reference + ": posting (" + request.getClass().getSimpleName() + ')');
	
		if (caches.getResponseUncommitted(reference).isPresent())
			throw new TransactionRejectedException("repeated request");
	
		createSemaphore(reference);
		postRequest(request);
	
		return reference;
	}

	/**
	 * Invalidates the caches, if needed, after the addition of the given response into store.
	 * 
	 * @param response the store
	 * @param classLoader the class loader of the transaction that computed {@code response}
	 * @throws ClassNotFoundException if some class cannot be found in the Takamaka code
	 */
	protected void invalidateCachesIfNeeded(TransactionResponse response, EngineClassLoader classLoader) throws ClassNotFoundException {
		caches.invalidateIfNeeded(response, classLoader);
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
			return new JarStoreInitialResponseBuilder(reference, (JarStoreInitialTransactionRequest) request, internal);
		else if (request instanceof GameteCreationTransactionRequest)
			return new GameteCreationResponseBuilder(reference, (GameteCreationTransactionRequest) request, internal);
    	else if (request instanceof JarStoreTransactionRequest)
    		return new JarStoreResponseBuilder(reference, (JarStoreTransactionRequest) request, internal);
    	else if (request instanceof ConstructorCallTransactionRequest)
    		return new ConstructorCallResponseBuilder(reference, (ConstructorCallTransactionRequest) request, internal);
    	else if (request instanceof AbstractInstanceMethodCallTransactionRequest aimctr)
    		return new InstanceMethodCallResponseBuilder(reference, aimctr, internal);
    	else if (request instanceof StaticMethodCallTransactionRequest)
    		return new StaticMethodCallResponseBuilder(reference, (StaticMethodCallTransactionRequest) request, internal);
    	else if (request instanceof InitializationTransactionRequest)
    		return new InitializationResponseBuilder(reference, (InitializationTransactionRequest) request, internal);
    	else
    		throw new TransactionRejectedException("Unexpected transaction request of class " + request.getClass().getName());
	}

	/**
	 * Node-specific implementation to post the given request. Each node should implement this,
	 * for instance by adding the request to some mempool or queue of requests to be executed.
	 * 
	 * @param request the request
	 */
	protected abstract void postRequest(TransactionRequest<?> request);

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
	 * Runs a callable and wraps any exception into an {@link TransactionRejectedException},
	 * if it is not a {@link TransactionException} nor a {@link CodeExecutionException}.
	 * 
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 * @throws TransactionException if the callable throws this
	 * @throws CodeExecutionException if the callable throws this
	 */
	private static <T> T wrapInCaseOfExceptionFull(Callable<T> what) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | CodeExecutionException | TransactionException e) {
			throw e;
		}
		catch (Throwable t) {
			LOGGER.log(Level.WARNING, "unexpected exception", t);
			throw new TransactionRejectedException(t);
		}
	}

	/**
	 * Runs a callable and wraps any exception into an {@link TransactionRejectedException},
	 * if it is not a {@link TransactionException}.
	 * 
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 * @throws TransactionException if the callable throws this
	 */
	private static <T> T wrapInCaseOfExceptionMedium(Callable<T> what) throws TransactionRejectedException, TransactionException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException | TransactionException e) {
			throw e;
		}
		catch (Throwable t) {
			LOGGER.log(Level.WARNING, "unexpected exception", t);
			throw new TransactionRejectedException(t);
		}
	}

	/**
	 * Runs a callable and wraps any exception into an {@link TransactionRejectedException}.
	 * 
	 * @param <T> the return type of the callable
	 * @param what the callable
	 * @return the return value of the callable
	 * @throws TransactionRejectedException the wrapped exception
	 */
	private static <T> T wrapInCaseOfExceptionSimple(Callable<T> what) throws TransactionRejectedException {
		try {
			return what.call();
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Unexpected exception", t);
			throw new TransactionRejectedException(t);
		}
	}

	private Optional<StorageValue> getOutcome(MethodCallTransactionResponse response) throws CodeExecutionException, TransactionException {
		if (response instanceof MethodCallTransactionSuccessfulResponse mctsr)
			return Optional.of(mctsr.getResult());
		else if (response instanceof MethodCallTransactionExceptionResponse mcter)
			throw new CodeExecutionException(mcter.getClassNameOfCause(), mcter.getMessageOfCause(), mcter.getWhere());
		else if (response instanceof MethodCallTransactionFailedResponse mctfr)
			throw new TransactionException(mctfr.getClassNameOfCause(), mctfr.getMessageOfCause(), mctfr.getWhere());
		else
			return Optional.empty(); // void methods return no value
	}

	/**
	 * Determines if the given transaction has not been committed yet.
	 * 
	 * @param transaction the transaction
	 * @return true if and only if that condition holds
	 * @throws NodeException if the node is not able to complete the operation
	 */
	private boolean isNotCommitted(TransactionReference transaction) throws NodeException {
		try {
			getResponse(transaction);
			return false;
		}
		catch (TransactionRejectedException | UnknownReferenceException e) {
			return true;
		}
	}

	/**
	 * Takes note that a new transaction has been delivered. This transaction is
	 * not a {@code @@View} transaction.
	 * 
	 * @param request the request of the transaction
	 * @param response the response computed for {@code request}
	 */
	private void takeNoteForNextReward(TransactionRequest<?> request, TransactionResponse response) {
		if (!(request instanceof SystemTransactionRequest)) {
			numberOfTransactionsSinceLastReward = numberOfTransactionsSinceLastReward.add(ONE);

			if (response instanceof NonInitialTransactionResponse) {
				var responseAsNonInitial = (NonInitialTransactionResponse) response;
				BigInteger gasConsumedButPenalty = responseAsNonInitial.getGasConsumedForCPU()
						.add(responseAsNonInitial.getGasConsumedForStorage())
						.add(responseAsNonInitial.getGasConsumedForRAM());

				gasConsumedSinceLastReward = gasConsumedSinceLastReward.add(gasConsumedButPenalty);

				BigInteger gasConsumedTotal = gasConsumedButPenalty;
				if (response instanceof FailedTransactionResponse)
					gasConsumedTotal = gasConsumedTotal.add(((FailedTransactionResponse) response).getGasConsumedForPenalty());

				BigInteger gasPrice = ((NonInitialTransactionRequest<?>) request).getGasPrice();
				BigInteger reward = gasConsumedTotal.multiply(gasPrice);
				coinsSinceLastRewardWithoutInflation = coinsSinceLastRewardWithoutInflation.add(reward);

				gasConsumedTotal = addInflation(gasConsumedTotal);
				reward = gasConsumedTotal.multiply(gasPrice);
				coinsSinceLastReward = coinsSinceLastReward.add(reward);
			}
		}
	}

	private BigInteger addInflation(BigInteger gas) {
		Optional<Long> currentInflation = caches.getCurrentInflation();

		if (currentInflation.isPresent())
			gas = gas.multiply(_100_000_000.add(BigInteger.valueOf(currentInflation.get())))
					 .divide(_100_000_000);

		return gas;
	}

	private void scheduleForNotificationOfEvents(TransactionResponse response) {
		if (response instanceof TransactionResponseWithEvents) {
			var responseWithEvents = (TransactionResponseWithEvents) response;
			if (responseWithEvents.getEvents().count() > 0L)
				scheduleForNotificationOfEvents(responseWithEvents);
		}
	}

	/**
	 * Creates a semaphore for those who will wait for the result of the given request.
	 * 
	 * @param reference the reference of the transaction for the request
	 */
	private void createSemaphore(TransactionReference reference) {
		if (semaphores.putIfAbsent(reference, new Semaphore(0)) != null)
			throw new IllegalStateException("repeated request");
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
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}

	/**
	 * The view of the node with the methods that are useful inside this module.
	 * This avoids to export such methods as public elsewhere.
	 */
	private class NodeInternalImpl implements NodeInternal {

		@Override
		public LocalNodeConfig<?,?> getConfig() {
			return config;
		}

		@Override
		public NodeCache getCaches() {
			return caches;
		}

		@Override
		public GasCostModel getGasCostModel() {
			return gasCostModel;
		}

		@Override
		public Store<?> getStore() {
			return store;
		}

		public void setStore(Store<?> store) {
			AbstractLocalNodeImpl.this.store = (S) store; // TODO
		}

		@Override
		public StoreUtility getStoreUtilities() {
			return storeUtilities;
		}

		@Override
		public TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, NodeException {
			return AbstractLocalNodeImpl.this.getRequest(reference);
		}

		@Override
		public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, UnknownReferenceException, NodeException {
			return AbstractLocalNodeImpl.this.getResponse(reference);
		}

		@Override
		public ClassTag getClassTag(StorageReference object) throws NodeException, UnknownReferenceException {
			return AbstractLocalNodeImpl.this.getClassTag(object);
		}

		@Override
		public Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
			return AbstractLocalNodeImpl.this.runInstanceMethodCallTransaction(request);
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return executors.submit(task);
		}

		@Override
		public void submit(Runnable task) {
			executors.submit(task);
		}

		@Override
		public long getNow() {
			return AbstractLocalNodeImpl.this.getNow();
		}
	}
}