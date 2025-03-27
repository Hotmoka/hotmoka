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
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.AbstractAutoCloseableWithLockAndOnCloseHandlers;
import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.CodeFutures;
import io.hotmoka.node.JarFutures;
import io.hotmoka.node.SubscriptionsManagers;
import io.hotmoka.node.TransactionReferences;
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
import io.hotmoka.node.api.requests.MethodCallTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.LocalNode;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreException;

/**
 * Partial implementation of a local (ie., non-remote) node.
 * 
 * @param <N> the type of this node
 * @param <C> the type of the configuration of this node
 * @param <S> the type of the store of this node
 * @param <T> the type of the store transformations that can be started from the store of this node
 */
@ThreadSafe
public abstract class AbstractLocalNodeImpl<N extends AbstractLocalNodeImpl<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractStoreImpl<N,C,S,T>, T extends AbstractStoreTransformationImpl<N,C,S,T>> extends AbstractAutoCloseableWithLockAndOnCloseHandlers<ClosedNodeException> implements LocalNode<C> {

	/**
	 * The manager of the subscriptions to the events occurring in this node.
	 */
	private final SubscriptionsManager subscriptions = SubscriptionsManagers.create();

	/**
	 * The configuration of the node.
	 */
	private final C config;

	/**
	 * The hasher for transaction requests.
	 */
	private final Hasher<TransactionRequest<?>> hasher;

	/**
	 * A map that provides a semaphore for each currently executing transaction.
	 * It is released when the check transaction fails or when the deliver transaction terminates.
	 * It is used to know when to start polling for the response of a request.
	 * Without waiting for that moment, polling might start too early, which results
	 * either in timeouts (the polled response does not arrive because delivering
	 * is very slow) or in delayed answers (the transaction has been delivered,
	 * but the polling process had increased the polling time interval so much that
	 * it waits for extra time before checking for the delivered transaction).
	 * See how this works inside {@link #getPolledResponse(TransactionReference)}.
	 */
	private final ConcurrentMap<TransactionReference, Semaphore> semaphores;

	/**
	 * An executor for background tasks.
	 */
	private final ExecutorService executors;

	/**
	 * Cached error messages of requests that failed their {@link #checkTransaction(TransactionRequest)}.
	 * This is useful to avoid polling for the outcome of recent requests whose
	 * {@link #checkTransaction(TransactionRequest)} failed, hence never
	 * got the chance to pass to delivering.
	 */
	private final LRUCache<TransactionReference, String> recentlyRejectedTransactionsMessages = new LRUCache<>(100, 1000);

	/**
	 * The version of Hotmoka used by the nodes.
	 */
	public final static String HOTMOKA_VERSION;

	private final static Logger LOGGER = Logger.getLogger(AbstractLocalNodeImpl.class.getName());

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

	/**
	 * Creates a new node.
	 * 
	 * @param config the configuration of the node
	 * @param init if true, the working directory of the node gets initialized
	 * @throws NodeException if the operation cannot be completed correctly
	 */
	protected AbstractLocalNodeImpl(C config, boolean init) throws NodeException {
		super(ClosedNodeException::new);

		this.config = config;
		this.semaphores = new ConcurrentHashMap<>();

		try {
			this.hasher = HashingAlgorithms.sha256().getHasher(TransactionRequest::toByteArray);
		}
		catch (NoSuchAlgorithmException e) {
			throw new NodeException(e);
		}

		if (init)
			initWorkingDirectory();

		this.executors = Executors.newCachedThreadPool();
	}

	@Override
	public final void close() throws NodeException {
		try {
			if (stopNewCalls())
				closeResources();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public final Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler) throws NodeException {
		try (var scope = mkScope()) {
			return subscriptions.subscribeToEvents(creator, handler);
		}
	}

	@Override
	public final ConsensusConfig<?,?> getConfig() throws NodeException, InterruptedException {
		S store = enterHead();

		try (var scope = mkScope()) {
			return store.getConfig();
		}
		finally {
			exit(store);
		}
	}

	@Override
	public final C getLocalConfig() throws NodeException {
		try (var scope = mkScope()) {
			return config;
		}
	}

	@Override
	public final TransactionReference getTakamakaCode() throws NodeException, InterruptedException {
		try (var scope = mkScope()) {
			var manifest = getManifest();

			try {
				return getClassTag(manifest).getJar();
			}
			catch (UnknownReferenceException e) {
				throw new NodeException("The manifest of the node cannot be found in the node itself: is the node initialized?", e);
			}
		}
	}

	@Override
	public final StorageReference getManifest() throws NodeException, InterruptedException {
		S store = enterHead();

		try (var scope = mkScope()) {
			return store.getManifest().orElseThrow(() -> new NodeException("The node is not initialized yet"));
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
		finally {
			exit(store);
		}
	}

	@Override
	public final TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		try (var scope = mkScope()) {
			Semaphore semaphore = semaphores.get(Objects.requireNonNull(reference));
			if (semaphore != null)
				// if we are polling for the outcome of a request sent to this same node, it is better
				// to wait until it is delivered (or its checking fails) and start polling only after:
				// this optimizes the time of waiting
				semaphore.acquire(); // TODO: possibly introduce a timeout here

			var attempts = config.getMaxPollingAttempts();
			for (long attempt = 1, delay = config.getPollingDelay(); attempt <= Math.max(1L, attempts); attempt++, delay = delay * 110 / 100) {
				S store = enterHead();

				try {
					return store.getResponse(reference);
				}
				catch (UnknownReferenceException e) {
					String rejectionMessage = recentlyRejectedTransactionsMessages.get(reference);
					if (rejectionMessage != null)
						throw new TransactionRejectedException(rejectionMessage, store.getConfig());
				}
				catch (StoreException e) {
					throw new NodeException(e);
				}
				finally {
					exit(store);
				}

				// the response is not available yet, nor did it get rejected: we wait a bit
				Thread.sleep(delay);
			}

			throw new TimeoutException("Cannot find the response of transaction reference " + reference + ": tried " + attempts + " times");
		}
	}

	@Override
	public final TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, NodeException, InterruptedException {
		S store = enterHead();
		
		try (var scope = mkScope()) {
			return store.getRequest(Objects.requireNonNull(reference));
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
		finally {
			exit(store);
		}
	}

	@Override
	public final TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, NodeException, InterruptedException {
		S store = enterHead();

		try (var scope = mkScope()) {
			return store.getResponse(Objects.requireNonNull(reference));
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
		finally {
			exit(store);
		}
	}

	@Override
	public final ClassTag getClassTag(StorageReference reference) throws UnknownReferenceException, NodeException, InterruptedException {
		S store = enterHead();

		try (var scope = mkScope()) {
			Objects.requireNonNull(reference);

			if (store.getResponse(reference.getTransaction()) instanceof TransactionResponseWithUpdates trwu)
				return trwu.getUpdates()
					.filter(update -> update instanceof ClassTag && update.getObject().equals(reference))
					.map(update -> (ClassTag) update)
					.findFirst()
					.orElseThrow(() -> new NodeException("Object " + reference + " has no class tag in store"));
			else
				throw new NodeException("The transaction that created object " + reference + " does not contain updates");
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
		finally {
			exit(store);
		}
	}

	@Override
	public final Stream<Update> getState(StorageReference reference) throws UnknownReferenceException, NodeException, InterruptedException {
		S store = enterHead();

		try (var scope = mkScope()) {
			try {
				Stream<TransactionReference> history = store.getHistory(Objects.requireNonNull(reference));
				var updates = new HashSet<Update>();
				CheckRunnable.check(NodeException.class, () -> history.forEachOrdered(UncheckConsumer.uncheck(NodeException.class,
					transaction -> collectUpdates(store, reference, transaction, updates))));

				return updates.stream();
			}
			catch (StoreException e) {
				throw new NodeException(e);
			}
		}
		finally {
			exit(store);
		}
	}

	@Override
	public final TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException {
		try (var scope = mkScope()) {
			TransactionReference reference = post(request);
			getPolledResponse(reference);
			return reference;
		}
	}

	@Override
	public final void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		try (var scope = mkScope()) {
			getPolledResponse(post(request));
		}
	}

	@Override
	public final StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		try (var scope = mkScope()) {
			var response = getPolledResponse(post(request));
			if (response instanceof GameteCreationTransactionResponse gctr)
				return gctr.getGamete();
			else
				throw new NodeException("Wrong type " + response.getClass().getName() + " for the response of a gamete creation request");
		}
	}

	@Override
	public final TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException, NodeException, TimeoutException, InterruptedException {
		try (var scope = mkScope()) {
			return postJarStoreTransaction(request).get();
		}
	}

	@Override
	public final StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, InterruptedException, NodeException, TimeoutException {
		try (var scope = mkScope()) {
			return postConstructorCallTransaction(request).get();
		}
	}

	@Override
	public final Optional<StorageValue> addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		try (var scope = mkScope()) {
			return postInstanceMethodCallTransaction(request).get();
		}
	}

	@Override
	public final Optional<StorageValue> addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		try (var scope = mkScope()) {
			return postStaticMethodCallTransaction(request).get();
		}
	}

	@Override
	public final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException {
		S store = enterHead();

		try (var scope = mkScope()) {
			var reference = TransactionReferences.of(hasher.hash(request));
			String referenceAsString = reference.toString();
			LOGGER.info(referenceAsString + ": running start (" + request.getClass().getSimpleName() + " -> " + trim(request.getStaticTarget().getName()) + ')');
			Optional<StorageValue> result = store.beginViewTransformation().runInstanceMethodCallTransaction(request, reference);
			LOGGER.info(referenceAsString + ": running success");
			return result;
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
		finally {
			exit(store);
		}
	}

	@Override
	public final Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, InterruptedException {
		S store = enterHead();

		try (var scope = mkScope()) {
			var reference = TransactionReferences.of(hasher.hash(request));
			String referenceAsString = reference.toString();
			LOGGER.info(referenceAsString + ": running start (" + request.getClass().getSimpleName() + " -> " + trim(request.getStaticTarget().getName()) + ')');
			Optional<StorageValue> result = store.beginViewTransformation().runStaticMethodCallTransaction(request, reference);
			LOGGER.info(referenceAsString + ": running success");
			return result;
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
		finally {
			exit(store);
		}
	}

	@Override
	public final JarFuture postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		try (var scope = mkScope()) {
			return JarFutures.of(post(request), this);
		}
	}

	@Override
	public final ConstructorFuture postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		try (var scope = mkScope()) {
			return CodeFutures.ofConstructor(post(request), this);
		}
	}

	@Override
	public final MethodFuture postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		try (var scope = mkScope()) {
			return CodeFutures.ofMethod(post(request), this);
		}
	}

	@Override
	public final MethodFuture postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		try (var scope = mkScope()) {
			return CodeFutures.ofMethod(post(request), this);
		}
	}

	/**
	 * Called when this node is executing something that needs the store of the head.
	 * It can be used, for instance, to take note that that store cannot be
	 * garbage-collected from that moment.
	 * 
	 * @return the entered store of the head
	 * @throws NodeException if the operation could not be completed correctly
	 * @throws InterruptedException if the current thread is interrupted while waiting for the result
	 */
	protected abstract S enterHead() throws NodeException, InterruptedException;

	/**
	 * Called when this node finished executing something that needed the given store.
	 * 
	 * @param store the store
	 * @throws NodeException if the operation could not be completed correctly
	 */
	protected void exit(S store) throws NodeException {}

	/**
	 * Yields the executors that can be used to start tasks with this node.
	 * 
	 * @return the executors
	 */
	protected final ExecutorService getExecutors() {
		return executors;
	}

	/**
	 * Yields the hasher of transactions to use with this node.
	 * 
	 * @return the hasher of transactions
	 */
	protected final Hasher<TransactionRequest<?>> getHasher() {
		return hasher;
	}

	/**
	 * Takes note that this node has rejected a given transaction request.
	 * 
	 * @param request the rejected transaction request
	 * @param e the exception that explains why it has been rejected
	 */
	protected final void signalRejected(TransactionRequest<?> request, TransactionRejectedException e) {
		var reference = TransactionReferences.of(hasher.hash(request));
		recentlyRejectedTransactionsMessages.put(reference, e.getMessage());
		signalCompleted(reference);
	}

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 * @throws NodeException if this node is not able to perform the operation
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 */
	protected final void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException, NodeException, InterruptedException {
		S store = enterHead();

		try {
			store.checkTransaction(request);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
		finally {
			exit(store);
		}
	}

	/**
	 * Publishes the given transaction, that is, takes note that it has been added to the store
	 * of this node and became visible to its users. This method will signal all tasks waiting
	 * for the completion of the transaction and will trigger all events contained
	 * in the transaction. This method will be called, for instance, when one or more blocks
	 * are added to the main chain of a blockchain, for each of the transactions in such blocks.
	 * 
	 * @param reference the transaction to publish
	 * @param store the store where the transaction and its potential events can be found
	 * @throws NodeException if this node is not able to perform the operation
	 * @throws UnknownReferenceException if {@code reference} cannot be found in {@code store}
	 */
	protected final void publish(TransactionReference reference, S store) throws NodeException, UnknownReferenceException {
		signalCompleted(reference);

		try {
			if (store.getResponse(reference) instanceof TransactionResponseWithEvents trwe && trwe.hasEvents())
				for (var event: trwe.getEvents().toArray(StorageReference[]::new))
					notifyEvent(event, store);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	protected void publishAllTransactionsDeliveredIn(T transformation, S store) throws NodeException {
		for (var tx: transformation.getDeliveredTransactions().toArray(TransactionReference[]::new)) {
			try {
				publish(tx, store);
			}
			catch (UnknownReferenceException e) {
				// the transactions have been delivered, if they cannot be found then there is a problem in the database
				throw new NodeException("Delivered transactions should be in store", e);
			}
		}
	}

	/**
	 * Closes all the resources of this node.
	 * 
	 * @throws NodeException if this node is not able to perform the operation
	 */
	protected void closeResources() throws NodeException {
		executors.shutdownNow();
	}

	/**
	 * Creates an empty store for this node, with empty cache.
	 * 
	 * @return the empty store
	 * @throws NodeException if this node is not able to perform the operation
	 */
	protected abstract S mkEmptyStore() throws NodeException;

	/**
	 * Node-specific implementation to post the given request. Each node should implement this,
	 * for instance by adding the request to some mempool or queue of requests to be executed.
	 * 
	 * @param request the request
	 * @throws NodeException if this node is not able to perform the operation
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation could not be completed in time
	 */
	protected abstract void postRequest(TransactionRequest<?> request) throws NodeException, InterruptedException, TimeoutException;

	/**
	 * Cleans the directory where the node's data live.
	 * 
	 * @throws NodeException if this node is not able to perform the operation
	 */
	private void initWorkingDirectory() throws NodeException {
		try {
			deleteRecursively(config.getDir());
			Files.createDirectories(config.getDir());
		}
		catch (IOException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Wakes up who was waiting for the outcome of the given transaction.
	 * 
	 * @param reference the reference of the transaction
	 */
	private void signalCompleted(TransactionReference reference) {
		Semaphore semaphore = semaphores.remove(reference);
		if (semaphore != null)
			semaphore.release();
	}

	private void notifyEvent(StorageReference event, S store) throws NodeException {
		StorageReference creator;

		try {
			creator = store.getCreator(event);
		}
		catch (StoreException | UnknownReferenceException | FieldNotFoundException e) {
			// this private method is only called on events in responses in store:
			// if they cannot be processed, there is a problem in the database
			throw new NodeException(e);
		}

		subscriptions.notifyEvent(creator, event);
		LOGGER.info(event + ": notified as event with creator " + creator);		
	}

	/**
	 * Posts the given request. It does some preliminary preparation then calls
	 * {@link #postRequest(TransactionRequest)}, that will implement the node-specific logic of posting.
	 * 
	 * @param request the request
	 * @return the reference of the request
	 * @throws TransactionRejectedException if the request was already present in the store
	 */
	private TransactionReference post(TransactionRequest<?> request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException {
		var reference = TransactionReferences.of(hasher.hash(request));
		String simpleNameOfRequest = request.getClass().getSimpleName();

		if (request instanceof MethodCallTransactionRequest mctr)
			LOGGER.info(reference + ": posting (" + simpleNameOfRequest + " -> " + trim(mctr.getStaticTarget().getName()) + ')');
		else if (request instanceof ConstructorCallTransactionRequest cctr)
			LOGGER.info(reference + ": posting (" + simpleNameOfRequest + " -> " + trim(cctr.getStaticTarget().getDefiningClass().getName()) + ')');
		else
			LOGGER.info(reference + ": posting (" + simpleNameOfRequest + ')');

		S store = enterHead();

		try {
			store.getResponse(reference);
			// if the response is found, then no exception is thrown above, which means that the request was repeated
			throw new TransactionRejectedException("Repeated request " + reference, store.getConfig());
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
		catch (UnknownReferenceException e) {
			// this is fine: there was no previous request with the same reference so we register
			// its semaphore and post the request for execution
			createSemaphore(store, reference);
			postRequest(request);

			return reference;
		}
		finally {
			exit(store);
		}
	}

	private static String trim(String s) {
		return s.length() > 50 ? s.substring(0, 50) + "..." : s;
	}

	/**
	 * Collects, into the given set, the updates of the fields of an object
	 * occurred during the execution of a given transaction.
	 * 
	 * @param store the store of this node
	 * @param object the reference to the object
	 * @param reference the reference to the transaction
	 * @param updates the set where they must be added
	 * @throws NodeException if this node is misbehaving
	 */
	private void collectUpdates(S store, StorageReference object, TransactionReference reference, Set<Update> updates) throws NodeException {
		try {
			if (store.getResponse(reference) instanceof TransactionResponseWithUpdates trwu)
				trwu.getUpdates()
					.filter(update -> update.getObject().equals(object) && updates.stream().noneMatch(update::sameProperty))
					.forEach(updates::add);
			else
				throw new NodeException("Reference " + reference + " is part of the histories but did not generate updates");
		}
		catch (UnknownReferenceException e) {
			throw new NodeException("Reference " + reference + " is part of the histories but is not in the store");
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Creates a semaphore for those who will wait for the result of a request.
	 * 
	 * @param reference the reference of the transaction of the request
	 * @throws TransactionRejectedException if there is already a semaphore for {@code reference}, which
	 *                                      means that the request is repeated
	 */
	private void createSemaphore(S store, TransactionReference reference) throws TransactionRejectedException {
		if (semaphores.putIfAbsent(reference, new Semaphore(0)) != null)
			throw new TransactionRejectedException("Repeated request " + reference, store.getConfig());
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
}