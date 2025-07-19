/*
Copyright 2024 Fausto Spoto

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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.NonInitialTransactionRequest;
import io.hotmoka.node.api.requests.SystemTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.FailedTransactionResponse;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.LocalNodeException;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreTransformation;
import io.hotmoka.node.local.internal.builders.ExecutionEnvironment;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * Partial implementation of a store transformation. This is not thread-safe hence it must
 * be used by a thread at a time or shared under synchronization.
 * 
 * @param <N> the type of the node whose store performs this transformation
 * @param <C> the type of the configuration of the node whose store performs this transformation
 * @param <S> the type of the store that performs this transformation
 * @param <T> the type of this store transformation
 */
public abstract class AbstractStoreTransformationImpl<N extends AbstractLocalNodeImpl<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractStoreImpl<N,C,S,T>, T extends AbstractStoreTransformationImpl<N,C,S,T>> extends ExecutionEnvironment implements StoreTransformation<S, T> {
	private final static Logger LOGGER = Logger.getLogger(AbstractStoreTransformationImpl.class.getName());

	/**
	 * The store from which the transformation started.
	 */
	private final S store;

	/**
	 * The requests added during this transformation. They are kept in order of addition.
	 */
	private final LinkedHashMap<TransactionReference, TransactionRequest<?>> deltaRequests = new LinkedHashMap<>();

	/**
	 * The responses added during this transformation.
	 */
	private final Map<TransactionReference, TransactionResponse> deltaResponses = new HashMap<>();

	/**
	 * The histories of the objects created during this transformation.
	 */
	private final Map<StorageReference, TransactionReference[]> deltaHistories = new HashMap<>();

	/**
	 * The storage reference of the manifest added during this transformation, if any.
	 */
	private volatile StorageReference deltaManifest;

	/**
	 * The cache used during this transformation.
	 */
	private volatile StoreCache cache;

	/**
	 * The gas consumed for CPU execution, RAM or storage in this transformation.
	 */
	private volatile BigInteger gasConsumed;

	/**
	 * The reward to send to the miners, accumulated during this transformation.
	 */
	private volatile BigInteger reward;

	/**
	 * The current time to use for the execution of transactions delivered into this transformation.
	 */
	private final long now;

	/**
	 * Enough gas for a simple get method.
	 */
	protected final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	protected final static BigInteger _100_000_000 = BigInteger.valueOf(100_000_000L);

	/**
	 * Creates a transformation whose transactions that starts from the given store.
	 * 
	 * @param store the initial store of the transformation
	 * @param consensus the consensus to use for the execution of transactions in the transformation
	 * @param now the current time to use for the execution of the transactions delivered into the transformation
	 */
	protected AbstractStoreTransformationImpl(S store, ConsensusConfig<?,?> consensus, long now) {
		this.store = store;
		this.now = now;
		this.cache = store.getCache().setConfig(consensus);
		this.gasConsumed = BigInteger.ZERO;
		this.reward = BigInteger.ZERO;
	}

	@Override
	public final S getInitialStore() {
		return store;
	}

	@Override
	public final TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException, InterruptedException {
		var reference = TransactionReferences.of(getHasher().hash(request));
		String referenceAsString = reference.toString();
	
		try {
			LOGGER.info(referenceAsString + ": delivering start");

			var responseCreation = responseBuilderFor(reference, request).getResponseCreation();
			TransactionResponse response = responseCreation.getResponse();
			push(reference, request, response);
			responseCreation.replaceReverifiedResponses();
			takeNoteForNextReward(request, response);
			updateCaches(response, responseCreation.getClassLoader());
	
			LOGGER.info(referenceAsString + ": delivering success");
			return response;
		}
		catch (TransactionRejectedException e) {
			LOGGER.warning(referenceAsString + ": delivering failed: " + e.getMessage());
			throw e;
		}
	}

	@Override
	public final int deliveredCount() {
		return deltaRequests.size();
	}

	@Override
	public final TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException {
		// first we check in the delta, then in the initial store
		var request = deltaRequests.get(reference);
		return request != null ? request : store.getRequest(reference);
	}

	@Override
	public final TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException {
		// first we check in the delta, then in the initial store
		var response = deltaResponses.get(reference);
		return response != null ? response : store.getResponse(reference);
	}

	@Override
	public final Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException {
		// first we check in the delta, then in the initial store
		var history = deltaHistories.get(object);
		return history != null ? Stream.of(history) : store.getHistory(object);
	}

	@Override
	public final Optional<StorageReference> getManifest() {
		// first we check in the delta, then in the initial store
		StorageReference manifest = deltaManifest;
		return manifest != null ? Optional.of(manifest) : store.getManifest();
	}

	@Override
	public final long getNow() {
		return now;
	}

	@Override
	protected final <X> Future<X> submit(Callable<X> task) {
		return store.getNode().getExecutors().submit(task);
	}

	@Override
	protected StoreCache getCache() {
		return cache;
	}

	/**
	 * Yields the references of the transactions delivered into this transformation.
	 * 
	 * @return the references, in order of delivery
	 */
	protected final Stream<TransactionReference> getDeliveredTransactions() {
		// we force the order of insertion
		var result = new ArrayList<TransactionReference>();
		deltaRequests.forEach((key, _entry) -> result.add(key));
		return result.stream();
	}

	/**
	 * Yields the requests added during this transformation. They are kept in order of addition.
	 * 
	 * @return the requests
	 */
	protected final LinkedHashMap<TransactionReference, TransactionRequest<?>> getDeltaRequests() {
		return deltaRequests;
	}

	/**
	 * Yields the responses added during this transformation.
	 * 
	 * @return the responses
	 */
	protected final Map<TransactionReference, TransactionResponse> getDeltaResponses() {
		return deltaResponses;
	}

	/**
	 * Yields the histories of the objects created during this transformation.
	 * 
	 * @return the histories
	 */
	protected final Map<StorageReference, TransactionReference[]> getDeltaHistories() {
		return deltaHistories;
	}

	/**
	 * Yields the storage reference of the manifest added during this transformation, if any.
	 * 
	 * @return the storage reference of the manifest added during this transformation, if any
	 */
	protected final Optional<StorageReference> getDeltaManifest() {
		return Optional.ofNullable(deltaManifest);
	}

	@Override
	protected final Hasher<TransactionRequest<?>> getHasher() {
		return store.getHasher();
	}

	/**
	 * Yields the reward to send to the miners, accumulated during this transformation.
	 * 
	 * @return the reward
	 */
	protected final BigInteger getReward() {
		return reward;
	}

	/**
	 * Yields the gas consumed for CPU execution, RAM or storage in this transformation.
	 * 
	 * @return the gas consumed
	 */
	protected final BigInteger getGasConsumed() {
		return gasConsumed;
	}

	/**
	 * Updates the caches, if needed, after the addition of the given response into store.
	 * 
	 * @param response the response added to the store
	 * @param classLoader the class loader of the transaction that computed {@code response}
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	protected void updateCaches(TransactionResponse response, TakamakaClassLoader classLoader) throws InterruptedException {
		if (manifestMightHaveChanged(response)) {
			StorageReference manifest = getManifest().orElseThrow(() -> new LocalNodeException("The manifest has just been set, so it should have been found"));
			cache = cache.setValidators(extractValidators(manifest));
			LOGGER.info("the validators cache has been updated since it might have changed");
			cache = cache.setGasStation(extractGasStation(manifest));
			LOGGER.info("the gas station cache has been updated since it might have changed");
			cache = cache.setVersions(extractVersions(manifest));
			LOGGER.info("the versions manager cache has been updated since it might have changed");
		}

		if (consensusParametersMightHaveChanged(response, classLoader)) {
			StorageReference manifest = getManifest().orElseThrow(() -> new LocalNodeException("Some consensus parameter just changed, hence the manifest should be set"));
			long versionBefore = cache.getConfig().getVerificationVersion();
			cache = cache.setConfig(extractConsensus(manifest)).invalidateClassLoaders();
			long versionAfter = cache.getConfig().getVerificationVersion();
			LOGGER.info("the consensus parameters cache has been updated since it might have changed");

			if (versionBefore != versionAfter)
				LOGGER.info("the version of the verification module has changed from " + versionBefore + " to " + versionAfter);
		}

		if (gasPriceMightHaveChanged(response, classLoader)) {
			StorageReference manifest = getManifest().orElseThrow(() -> new LocalNodeException("The gas price just changed, hence the manifest should be set"));
			BigInteger newGasPrice = extractGasPrice(manifest);
			cache = cache.setGasPrice(newGasPrice);
			LOGGER.info("the gas cache has been updated since it might have changed: the new gas price is " + newGasPrice);
		}
	}

	/**
	 * Writes in this transformation the given response for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param response the response of the transaction
	 */
	protected final void setResponse(TransactionReference reference, TransactionResponse response) {
		deltaResponses.put(reference, response);
	}

	/**
	 * Yields the creator of the given event.
	 * 
	 * @param event the reference to the event; this is assumed to actually refer an event
	 * @return the creator of {@code event}
	 */
	protected final StorageReference getCreatorOfEvent(StorageReference event) {
		try {
			return getReferenceField(event, FieldSignatures.EVENT_CREATOR_FIELD);
		}
		catch (UnknownReferenceException | FieldNotFoundException e) {
			// since reference is assumed to refer to an event in store, it must exist and have a creator or otherwise the store is corrupted
			throw new LocalNodeException(e);
		}
	}

	/**
	 * Computes how many coins have been minted during the last reward:
	 * it is the price of the gas distributed minus the same price without inflation.
	 * 
	 * @param validators the validators object of the node
	 * @return the number of minted coins
	 */
	protected final BigInteger getCoinsMinted(StorageReference validators) {
		// we determine how many coins have been minted during the last reward
		BigInteger minted = BigInteger.ZERO; // TODO: implement inflationary start

		// it might happen that the last distribution goes beyond the limit imposed
		// as final supply: in that case we truncate the minted coins so that the current
		// supply reaches the final supply, exactly; this might occur from below (positive inflation)
		// or from above (negative inflation)
		BigInteger currentSupply = getCurrentSupply(validators);
		if (minted.signum() > 0) {
			BigInteger finalSupply = getConfig().getFinalSupply();
			BigInteger extra = finalSupply.subtract(currentSupply.add(minted));
			if (extra.signum() < 0)
				minted = minted.add(extra);
		}
		else if (minted.signum() < 0) {
			BigInteger finalSupply = getConfig().getFinalSupply();
			BigInteger extra = finalSupply.subtract(currentSupply.add(minted));
			if (extra.signum() > 0)
				minted = minted.add(extra);
		}

		return minted;
	}

	/**
	 * Yields the current total supply of the node, from its validators object.
	 * 
	 * @param validators the reference to the validators object of the node; this is assumed to
	 *                   actually refer to a validators object
	 * @return the total supply
	 */
	private BigInteger getCurrentSupply(StorageReference validators) {
		try {
			return getBigIntegerField(validators, FieldSignatures.ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD);
		}
		catch (UnknownReferenceException | FieldNotFoundException e) {
			// since reference is assumed to refer to a validators object in store, it must exist and have a currentSupply field
			throw new LocalNodeException(e);
		}
	}

	/**
	 * Writes in this transformation the given request for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param request the request of the transaction
	 */
	private void setRequest(TransactionReference reference, TransactionRequest<?> request) {
		deltaRequests.put(reference, request);
	}

	/**
	 * Sets the history of the given object, that is,
	 * the references to the transactions that provide information about
	 * its current state, in reverse chronological order (from newest to oldest).
	 * 
	 * @param object the object whose history is set
	 * @param history the transactions that will become the history of the object,
	 *                replacing its previous history; this is in chronological order,
	 *                from newest transactions to oldest; hence the last transaction is
	 *                that when the object has been created
	 */
	private void setHistory(StorageReference object, List<TransactionReference> history) {
		deltaHistories.put(object, history.toArray(TransactionReference[]::new));
	}

	/**
	 * Mark the node as initialized. This happens for initialization requests.
	 * 
	 * @param manifest the manifest to put in the node
	 */
	private void setManifest(StorageReference manifest) {
		this.deltaManifest = manifest;
	}

	/*
	 * Determine if the given response might change the value of some consensus parameter.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the value of some consensus parameters
	 */
	private boolean consensusParametersMightHaveChanged(TransactionResponse response, TakamakaClassLoader classLoader) {
		if (response instanceof InitializationTransactionResponse)
			return true;
		// we check if there are events of type ConsensusUpdate triggered by the manifest, validators, gas station or versions
		else if (response instanceof TransactionResponseWithEvents trwe && trwe.hasEvents()) {
			Optional<StorageReference> maybeManifest = getManifest();
	
			if (maybeManifest.isPresent()) {
				var manifest = maybeManifest.get();
				StorageReference validators = getValidators().orElseThrow(() -> new LocalNodeException("The manifest is set but the validators are not set"));
				StorageReference versions = getVersions().orElseThrow(() -> new LocalNodeException("The manifest is set but the versions are not set"));
				StorageReference gasStation = getGasStation().orElseThrow(() -> new LocalNodeException("The manifest is set but the gas station is not set"));

				for (var event: trwe.getEvents().toArray(StorageReference[]::new))
					if (isConsensusUpdateEvent(event, classLoader)) {
						StorageReference creator = getCreatorOfEvent(event);
						if (creator.equals(manifest) || creator.equals(validators) || creator.equals(gasStation) || creator.equals(versions))
							return true;
					}
			}
		}
	
		return false;
	}

	/**
	 * Checks if the given reference refers to a consensus update event. It assumes that the
	 * reference is actually in store.
	 * 
	 * @param event the reference to the event; this is assumed to be in store
	 * @param classLoader the class loader
	 * @return true if and only if that condition holds
	 */
	private boolean isConsensusUpdateEvent(StorageReference event, TakamakaClassLoader classLoader) {
		try {
			return classLoader.isConsensusUpdateEvent(getClassName(event));
		}
		catch (UnknownReferenceException e) {
			throw new LocalNodeException("Event " + event + " is not an object in store", e);
		}
		catch (ClassNotFoundException e) {
			// this event was created with this same classloader so we must find its class
			throw new LocalNodeException("Event " + event + " has an unknown class", e);
		}
	}

	/**
	 * Determines if the given response might change the gas price.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the gas price
	 */
	private boolean gasPriceMightHaveChanged(TransactionResponse response, TakamakaClassLoader classLoader) {
		if (response instanceof InitializationTransactionResponse)
			return true;
		else if (response instanceof TransactionResponseWithEvents trwe && trwe.hasEvents()) {
			Optional<StorageReference> maybeManifest = getManifest();
			
			if (maybeManifest.isPresent()) {
				StorageReference gasStation = getGasStation().orElseThrow(() -> new LocalNodeException("The manifest is set but the gas station is not set"));

				for (var event: trwe.getEvents().toArray(StorageReference[]::new))
					if (isGasPriceUpdateEvent(event, classLoader) && getCreatorOfEvent(event).equals(gasStation))
						return true;
			}
		}

		return false;
	}

	private boolean isGasPriceUpdateEvent(StorageReference event, TakamakaClassLoader classLoader) {
		try {
			return classLoader.isGasPriceUpdateEvent(getClassName(event));
		}
		catch (UnknownReferenceException e) {
			throw new LocalNodeException("Event " + event + " is not an object in store", e);
		}
		catch (ClassNotFoundException e) {
			throw new LocalNodeException("Event " + event + " has an unknown class", e);
		}
	}

	private boolean manifestMightHaveChanged(TransactionResponse response) {
		return response instanceof InitializationTransactionResponse;
	}

	/**
	 * Takes note that a new transaction has been delivered. This transaction is not a {@code @@View} transaction.
	 * 
	 * @param request the request of the transaction
	 * @param response the response computed for {@code request}
	 */
	private void takeNoteForNextReward(TransactionRequest<?> request, TransactionResponse response) {
		if (!(request instanceof SystemTransactionRequest) && response instanceof NonInitialTransactionResponse responseAsNonInitial) {
			BigInteger gasConsumedButPenalty = responseAsNonInitial.getGasConsumedForCPU()
				.add(responseAsNonInitial.getGasConsumedForStorage())
				.add(responseAsNonInitial.getGasConsumedForRAM());

			gasConsumed = gasConsumed.add(gasConsumedButPenalty);

			BigInteger gasConsumedTotal = gasConsumedButPenalty;
			if (response instanceof FailedTransactionResponse ftr)
				gasConsumedTotal = gasConsumedTotal.add(ftr.getGasConsumedForPenalty());

			if (!(request instanceof NonInitialTransactionRequest<?> nitr))
				throw new LocalNodeException("A non-initial transaction response has been computed for an initial transaction request of class " + request.getClass().getSimpleName());

			reward = reward.add(gasConsumedTotal.multiply(nitr.getGasPrice()));
		}
	}

	/**
	 * Pushes the result of executing a successful Hotmoka request.
	 * This method assumes that the given request was not already present in the store.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	private void push(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		if (response instanceof TransactionResponseWithUpdates trwu) {
			setRequest(reference, request);
			setResponse(reference, trwu);
			expandHistory(reference, trwu);
	
			if (response instanceof GameteCreationTransactionResponse gctr)
				LOGGER.info(reference + ": " + gctr.getGamete() + " created as gamete");
		}
		else if (response instanceof InitializationTransactionResponse) {
			if (request instanceof InitializationTransactionRequest itr) {
				setRequest(reference, request);
				setResponse(reference, response);
				StorageReference manifest = itr.getManifest();
				setManifest(manifest);
				LOGGER.info(reference + ": " + manifest + " set as manifest");
				LOGGER.info(reference + ": the node has been initialized");
			}
			else
				throw new LocalNodeException("Trying to initialize the node with a request of class " + request.getClass().getSimpleName());
		}
		else {
			setRequest(reference, request);
			setResponse(reference, response);
		}
	}

	/**
	 * Process the updates contained in the given response, expanding the history of the affected objects.
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
			.forEachOrdered(object -> setHistory(object, simplifiedHistory(object, reference, response.getUpdates())));
	}

	/**
	 * Adds the given transaction reference to the history of the given object and yields the simplified
	 * history. Simplification means that some elements of the previous history might not be useful anymore,
	 * since they get shadowed by the updates in the added transaction reference. This occurs when the values
	 * of some fields are updated in {@code added} and the useless old history element provided only values
	 * for the newly updated fields.
	 * 
	 * @param objectUpdatedInResponse the object whose history is being simplified
	 * @param added the transaction reference to add in front of the history of {@code object}
	 * @param addedUpdates the updates generated in {@code added}
	 * @return the simplified history, with {@code added} in front followed by a subset of the old history
	 */
	private List<TransactionReference> simplifiedHistory(StorageReference objectUpdatedInResponse, TransactionReference added, Stream<Update> addedUpdates) {
		// if the object has been created at the added transaction, that is its history
		if (objectUpdatedInResponse.getTransaction().equals(added))
			return List.of(added);

		Stream<TransactionReference> old;

		try {
			old = getHistory(objectUpdatedInResponse);
		}
		catch (UnknownReferenceException e) {
			// the object was created before this transaction: it must have a history or otherwise the store is corrupted
			throw new LocalNodeException("The computed response reports a modified object that is not in store", e);
		}

		// we trace the set of updates that are already covered by previous transactions, so that
		// subsequent history elements might become unnecessary, since they do not add any yet uncovered update
		Set<Update> covered = addedUpdates.filter(update -> update.getObject().equals(objectUpdatedInResponse)).collect(Collectors.toSet());
		var simplified = new ArrayList<TransactionReference>(10);
		simplified.add(added);
	
		var oldAsArray = old.toArray(TransactionReference[]::new);
		int lastPos = oldAsArray.length - 1;
		for (int pos = 0; pos < lastPos; pos++)
			addIfUncovered(oldAsArray[pos], objectUpdatedInResponse, covered, simplified);
	
		// the last is always useful, since it contains at least the class tag of the object
		if (lastPos >= 0)
			simplified.add(oldAsArray[lastPos]);
	
		return simplified;
	}

	/**
	 * Adds the given transaction reference to the history of the given object,
	 * if it provides updates for fields that have not yet been covered by other updates.
	 * 
	 * @param reference the transaction reference
	 * @param object the object
	 * @param covered the set of updates for the already covered fields
	 * @param history the history; this might be modified by the method, by prefixing {@code reference} at its front
	 */
	private void addIfUncovered(TransactionReference reference, StorageReference object, Set<Update> covered, List<TransactionReference> history) {
		if (getUpdates(reference).filter(update -> update.getObject().equals(object) && covered.stream().noneMatch(update::sameProperty) && covered.add(update)).count() > 0)
			history.add(reference);
	}
}