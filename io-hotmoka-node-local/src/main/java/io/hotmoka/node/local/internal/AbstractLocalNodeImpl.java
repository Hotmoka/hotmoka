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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
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
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.AbstractAutoCloseableWithLockAndOnCloseHandlers;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.instrumentation.GasCostModels;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.CodeFutures;
import io.hotmoka.node.JarFutures;
import io.hotmoka.node.SubscriptionsManagers;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.UninitializedNodeException;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConstructorFuture;
import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.MethodFuture;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.SubscriptionsManager;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.AbstractStore;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.LocalNode;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.ResponseBuilder;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.StoreTransaction;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * 
 * @param <C> the type of the configuration object used by the node
 * @param <S> the type of the store of the node
 */
@ThreadSafe
public abstract class AbstractLocalNodeImpl<N extends AbstractLocalNode<N,C,S>, C extends LocalNodeConfig<?,?>, S extends AbstractStore<S, N>> extends AbstractAutoCloseableWithLockAndOnCloseHandlers<ClosedNodeException> implements LocalNode<C> {

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

	public void notifyEvent(StorageReference creator, StorageReference event) {
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

	public final Hasher<TransactionRequest<?>> getHasher() {
		return hasher;
	}

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
	 * It is released when the check transaction fails or when the deliver transaction terminates.
	 * It is used to know when to start polling for the response of a request.
	 * Without waiting for that moment, polling might start too early, which results
	 * either in timeouts (the polled response does not arrive because delivering
	 * is very slow) or in delayed answers (the transaction has been delivered,
	 * but the polling process has increased the polling time interval so much that
	 * it waits for extra time because checking for the delivered transaction).
	 * See how this works inside {@link #getPolledResponse(TransactionReference)}.
	 */
	private final ConcurrentMap<TransactionReference, Semaphore> semaphores;

	/**
	 * An executor for short background tasks.
	 */
	private final ExecutorService executors;

	/**
	 * Cached error messages of requests that failed their {@link AbstractLocalNodeImpl#checkRequest(TransactionRequest)}.
	 * This is useful to avoid polling for the outcome of recent requests whose
	 * {@link #checkRequest(TransactionRequest)} failed, hence never
	 * got the chance to pass to {@link #deliverTransaction(TransactionRequest)}.
	 */
	private final LRUCache<TransactionReference, String> recentCheckRequestErrors;

	/**
	 * True if this blockchain has been already closed. Used to avoid double-closing in the shutdown hook.
	 */
	private final AtomicBoolean closed;

	/**
	 * Builds a node with a brand new, empty store.
	 * 
	 * @param config the configuration of the node
	 * @param consensus the consensus parameters at the beginning of the life of the node
	 */
	protected AbstractLocalNodeImpl(C config, ConsensusConfig<?,?> consensus) {
		this(config, Optional.of(consensus), true);
	}

	/**
	 * Builds a node, recycling a previous existing store. The store must be that
	 * of an already initialized node, whose consensus parameters are recovered from its manifest.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractLocalNodeImpl(C config) {
		this(config, Optional.empty(), false);
	}

	private AbstractLocalNodeImpl(C config, Optional<ConsensusConfig<?,?>> consensus, boolean deleteDir) {
		super(ClosedNodeException::new);

		this.config = config;

		try {
			this.hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unexpected exception", e);
		}

		this.recentCheckRequestErrors = new LRUCache<>(100, 1000);
		this.executors = Executors.newCachedThreadPool();
		this.semaphores = new ConcurrentHashMap<>();
		this.closed = new AtomicBoolean();

		if (deleteDir) {
			try {
				deleteRecursively(config.getDir());  // cleans the directory where the node's data live
				Files.createDirectories(config.getDir());
			}
			catch (IOException e) {
			}
		}

		if (consensus.isEmpty()) {
			try {
				S temp = mkStore(ValidatorsConsensusConfigBuilders.defaults().build());
				var storeTransaction = temp.beginTransaction(System.currentTimeMillis());
				storeTransaction.invalidateConsensusCache();
				consensus = Optional.of(storeTransaction.getConfigUncommitted());
				storeTransaction.abort();
			}
			catch (NoSuchAlgorithmException | StoreException e) {
				throw new RuntimeException(e); // TODO
			}
		}

		this.store = mkStore(consensus.get());

		addShutdownHook();
	}

	/**
	 * Factory method for creating the store of this node.
	 * 
	 * @return the store
	 */
	protected abstract S mkStore(ConsensusConfig<?,?> config);

	public final S getStore() {
		return store;
	}

	/**
	 * Yields the currently executing transaction.
	 * 
	 * @return the currently executing transaction
	 */
	public abstract StoreTransaction<S> getStoreTransaction();

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
			catch (StoreException e) {
				throw new NodeException(e);
			}
			finally {
				// we give five seconds
				executors.awaitTermination(5, TimeUnit.SECONDS);
			}
		}
	}

	@Override
	public final ConsensusConfig<?,?> getConfig() throws NodeException {
		try (var scope = mkScope()) {
			return store.getConfig();
		}
	}

	public final C getLocalNodeConfig() {
		return config;
	}

	public final GasCostModel getGasCostModel() {
		return gasCostModel;
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

	public void signalOutcomeIsReady(Stream<TransactionReference> references) {
		references.forEach(this::signalSemaphore);
	}

	@Override
	public final TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		try (var scope = mkScope()) {
			Objects.requireNonNull(reference);
			Semaphore semaphore = semaphores.get(reference);
			if (semaphore != null)
				// if we are polling for the outcome of a request sent to this same node, it is better
				// to wait until it is delivered (or its checking fails) and start polling only after:
				// this optimizes the time of waiting
				semaphore.acquire(); // TODO: possibly introduce a timeout here

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
				return store.getRequest(reference).orElseThrow(() -> new UnknownReferenceException(reference));
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
				Optional<TransactionResponse> response = store.getResponse(reference);
				if (response.isPresent())
					return response.get();

				// we check if the request passed its checkTransaction but failed its deliverTransaction:
				// in that case, the node contains the error message in its store; otherwise,
				// we check if the request did not pass its checkTransaction():
				// in that case, we might have its error message in {@link #recentCheckTransactionErrors}
				Optional<String> error = store.getError(reference).or(() -> getRecentCheckRequestErrorFor(reference));
				if (error.isPresent())
					throw new TransactionRejectedException(error.get());
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

			var maybeResponse = store.getResponse(reference.getTransaction());
			if (maybeResponse.isEmpty())
				throw new UnknownReferenceException(reference);
			else if (maybeResponse.get() instanceof TransactionResponseWithUpdates trwu)
				return trwu.getUpdates()
					.filter(update -> update instanceof ClassTag && update.getObject().equals(reference))
					.map(update -> (ClassTag) update)
					.findFirst()
					.orElseThrow(() -> new NodeException("Object " + reference + " has not class tag in store"));
			else
				throw new NodeException("The creation of object " + reference + " does not contain updates");
		}
	}

	@Override
	public final Stream<Update> getState(StorageReference reference) throws UnknownReferenceException, NodeException {
		try (var scope = mkScope()) {
			Objects.requireNonNull(reference);
			try {
				if (store.getResponse(reference.getTransaction()).isEmpty()) // TODO: remove after making history optional
					throw new UnknownReferenceException(reference);

				Stream<TransactionReference> history = store.getHistory(reference);
				var updates = new HashSet<Update>();
				CheckRunnable.check(StoreException.class, () -> history.forEachOrdered(UncheckConsumer.uncheck(transaction -> addUpdatesCommitted(reference, transaction, updates))));
				return updates.stream();
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
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		TransactionReference reference = post(request);
		getPolledResponse(reference);
		return reference;
	}

	@Override
	public final void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		getPolledResponse(post(request)); // result unused
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		var response = getPolledResponse(post(request));
		if (response instanceof GameteCreationTransactionResponse gctr)
			return gctr.getGamete();
		else
			throw new NodeException("Wrong type " + response.getClass().getName() + " for the response of a gamete creation request");
	}

	@Override
	public final TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException, NodeException, TimeoutException, InterruptedException {
		return postJarStoreTransaction(request).get();
	}

	@Override
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, InterruptedException, NodeException, TimeoutException {
		return postConstructorCallTransaction(request).get();
	}

	@Override
	public final Optional<StorageValue> addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return postInstanceMethodCallTransaction(request).get();
	}

	@Override
	public final Optional<StorageValue> addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return postStaticMethodCallTransaction(request).get();
	}

	@Override
	public final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException {
		try {
			var reference = TransactionReferences.of(hasher.hash(request));
			LOGGER.info(reference + ": running start (" + request.getClass().getSimpleName() + " -> " + request.getStaticTarget().getMethodName() + ')');

			Optional<StorageValue> result;

			var storeTransaction = store.beginTransaction(System.currentTimeMillis());
			result = storeTransaction.runInstanceMethodCallTransaction(request, reference);
			storeTransaction.abort();

			LOGGER.info(reference + ": running success");
			return result;
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public final Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException {
		try {
			var reference = TransactionReferences.of(hasher.hash(request));
			LOGGER.info(reference + ": running start (" + request.getClass().getSimpleName() + " -> " + request.getStaticTarget().getMethodName() + ')');
			Optional<StorageValue> result;

			var storeTransaction = store.beginTransaction(System.currentTimeMillis());
			result = storeTransaction.runStaticMethodCallTransaction(request, reference);
			storeTransaction.abort();

			LOGGER.info(reference + ": running success");
			return result;
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public final JarFuture postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return JarFutures.of(post(request), this);
	}

	@Override
	public final ConstructorFuture postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return CodeFutures.ofConstructor(post(request), this);
	}

	@Override
	public final MethodFuture postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return CodeFutures.ofMethod(post(request), this);
	}

	@Override
	public final MethodFuture postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return CodeFutures.ofMethod(post(request), this);
	}

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 * @throws NodeException 
	 */
	public final void checkRequest(TransactionRequest<?> request) throws TransactionRejectedException, NodeException {
		var reference = TransactionReferences.of(hasher.hash(request));
		if (getRecentCheckRequestErrorFor(reference).isPresent())
			throw new TransactionRejectedException("Repeated request " + reference);

		try {
			LOGGER.info(reference + ": checking start (" + request.getClass().getSimpleName() + ')');

			var storeTransaction = store.beginTransaction(System.currentTimeMillis());
			storeTransaction.responseBuilderFor(reference, request);
			storeTransaction.abort();

			LOGGER.info(reference + ": checking success");
		}
		catch (TransactionRejectedException e) {
			// we do not write the error message in the store, since a failed check request
			// means that nobody is paying for it and therefore we do not want to expand the store;
			// we just take note of the failure
			storeCheckRequestError(reference, e);
			LOGGER.warning(reference + ": checking failed: " + trimmedMessage(e));
			throw e;
		}
		catch (StoreException e) {
			storeCheckRequestError(reference, e);
			LOGGER.log(Level.WARNING, reference + ": checking failed with unexpected exception: " + e.getMessage());
			throw new NodeException(e);
		}
	}

	/**
	 * Builds a response for the given request and adds it to the store of the node.
	 * 
	 * @param request the request
	 * @return the response; if this node has a notion of commit, this response is typically still uncommitted
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	public final TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
		var reference = TransactionReferences.of(hasher.hash(request));

		try {
			var storeTransaction = getStoreTransaction();

			try {
				LOGGER.info(reference + ": delivering start (" + request.getClass().getSimpleName() + ')');

				ResponseBuilder<?,?> responseBuilder = storeTransaction.responseBuilderFor(reference, request);
				TransactionResponse response = responseBuilder.getResponse();
				storeTransaction.push(reference, request, response);
				responseBuilder.replaceReverifiedResponses();
				storeTransaction.scheduleEventsForNotificationAfterCommit(response);
				storeTransaction.takeNoteForNextReward(request, response);
				storeTransaction.invalidateCachesIfNeeded(response, responseBuilder.getClassLoader());

				LOGGER.info(reference + ": delivering success");
				return response;
			}
			catch (TransactionRejectedException e) {
				storeTransaction.push(reference, request, trimmedMessage(e));
				LOGGER.info(reference + ": delivering failed: " + trimmedMessage(e));
				LOGGER.log(Level.INFO, "transaction rejected", e);
				throw e;
			}
			catch (NodeException | UnknownReferenceException e) {
				storeTransaction.push(reference, request, trimmedMessage(e));
				LOGGER.log(Level.SEVERE, reference + ": delivering failed with unexpected exception", e);
				throw new RuntimeException(e);
			}
			catch (RuntimeException e) {
				storeTransaction.push(reference, request, trimmedMessage(e));
				LOGGER.log(Level.WARNING, reference + ": delivering failed with unexpected exception", e);
				throw e;
			}
		}
		catch (StoreException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	/**
	 * Yields the error message trimmed to a maximal length, to avoid overflow.
	 *
	 * @param t the throwable whose error message is processed
	 * @return the resulting message
	 */
	public final String trimmedMessage(Throwable t) {
		String message = t.getMessage();
		int length = message.length();
		int maxErrorLength = store.getConfig().getMaxErrorLength(); // TODO: uncommitted?
		return length <= maxErrorLength ? message : (message.substring(0, maxErrorLength) + "...");
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
	
		if (store.getResponse(reference).isPresent())
			throw new TransactionRejectedException("Repeated request " + reference);
	
		createSemaphore(reference);
		postRequest(request);
	
		return reference;
	}

	public final void setStore(S store) {
		this.store = store; // TODO
	}

	/**
	 * Node-specific implementation to post the given request. Each node should implement this,
	 * for instance by adding the request to some mempool or queue of requests to be executed.
	 * 
	 * @param request the request
	 */
	protected abstract void postRequest(TransactionRequest<?> request);

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return executors.submit(task);
	}

	private Optional<String> getRecentCheckRequestErrorFor(TransactionReference reference) {
		return Optional.ofNullable(recentCheckRequestErrors.get(reference));
	}

	private void storeCheckRequestError(TransactionReference reference, Throwable e) {
		recentCheckRequestErrors.put(reference, trimmedMessage(e));
	}

	/**
	 * Adds, to the given set, the updates of the fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param updates the set where they must be added
	 * @throws StoreException 
	 */
	private void addUpdatesCommitted(StorageReference object, TransactionReference transaction, Set<Update> updates) throws StoreException {
		Optional<TransactionResponse> maybeResponse = store.getResponse(transaction);
		if (maybeResponse.isEmpty())
			throw new StoreException("Storage reference " + transaction + " is part of the history of an object but it is not in the store");
		else if (maybeResponse.get() instanceof TransactionResponseWithUpdates trwu)
			trwu.getUpdates()
				.filter(update -> update.getObject().equals(object) && updates.stream().noneMatch(update::sameProperty))
				.forEach(updates::add);
		else
			throw new StoreException("Storage reference " + transaction + " is part of the history of an object but it did not generate updates");
	}

	/**
	 * Creates a semaphore for those who will wait for the result of the given request.
	 * 
	 * @param reference the reference of the transaction for the request
	 * @throws TransactionRejectedException 
	 */
	private void createSemaphore(TransactionReference reference) throws TransactionRejectedException {
		if (semaphores.putIfAbsent(reference, new Semaphore(0)) != null)
			throw new TransactionRejectedException("Repeated request " + reference);
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
}