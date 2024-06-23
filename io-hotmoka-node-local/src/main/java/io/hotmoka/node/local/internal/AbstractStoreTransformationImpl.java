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

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
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
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.ResponseBuilder;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.StoreTransformation;
import io.hotmoka.node.local.internal.builders.ExecutionEnvironment;

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
	private volatile StorageReference manifest;

	/**
	 * The cache used during this transformation.
	 */
	private volatile StoreCache cache;

	/**
	 * The gas consumed for CPU execution, RAM or storage in this transformation.
	 */
	private volatile BigInteger gasConsumed;

	/**
	 * The reward to send to the validators, accumulated during this transformation.
	 */
	private volatile BigInteger coins;

	/**
	 * The reward to send to the validators, accumulated during this transformation, without considering the inflation.
	 */
	private volatile BigInteger coinsWithoutInflation;

	/**
	 * The current time to use for the execution of transactions delivered into this transformation.
	 */
	private final long now;

	/**
	 * Enough gas for a simple get method.
	 */
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	private final static BigInteger _100_000_000 = BigInteger.valueOf(100_000_000L);

	/**
	 * Creates a transformation whose transactions are executed with the given executors.
	 * 
	 * @param store the initial store of the transformation
	 * @param consensus the consensus to use for the execution of transactions in the transformation
	 * @param now the current time to use for the execution of delivered transactions into the transformation
	 */
	protected AbstractStoreTransformationImpl(S store, ConsensusConfig<?,?> consensus, long now) {
		super(store.getNode().getExecutors());

		this.store = store;
		this.now = now;
		this.cache = store.getCache().setConfig(consensus);
		this.gasConsumed = BigInteger.ZERO;
		this.coins = BigInteger.ZERO;
		this.coinsWithoutInflation = BigInteger.ZERO;
	}

	@Override
	public final S getInitialStore() {
		return store;
	}

	@Override
	public final S getFinalStore() throws StoreException {
		return store.addDelta(cache, deltaRequests, deltaResponses, deltaHistories, Optional.ofNullable(manifest));
	}

	@Override
	public final void deliverRewardTransaction(String behaving, String misbehaving) throws StoreException {
		try {
			Optional<StorageReference> maybeManifest = getManifest();
			if (maybeManifest.isPresent()) {
				LOGGER.info("reward distribution: behaving validators: " + behaving + ", misbehaving validators: " + misbehaving);

				// we use the manifest as caller, since it is an externally-owned account
				StorageReference manifest = maybeManifest.get();
				BigInteger nonce = getNonce(manifest);
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
	
				// we determine how many coins have been minted during the last reward:
				// it is the price of the gas distributed minus the same price without inflation
				BigInteger minted = coins.subtract(coinsWithoutInflation);
	
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

				var request = TransactionRequests.instanceSystemMethodCall
					(manifest, nonce, _100_000, takamakaCode, MethodSignatures.VALIDATORS_REWARD, validators,
					StorageValues.bigIntegerOf(coins), StorageValues.bigIntegerOf(minted),
					StorageValues.stringOf(behaving), StorageValues.stringOf(misbehaving),
					StorageValues.bigIntegerOf(gasConsumed), StorageValues.bigIntegerOf(deliveredCount()));
	
				TransactionResponse response = responseBuilderFor(TransactionReferences.of(store.getHasher().hash(request)), request).getResponse();
				// if there is only one update, it is the update of the nonce of the manifest: we prefer not to expand
				// the store with the transaction, so that the state stabilizes, which might give
				// to the node the chance of suspending the generation of new blocks
				if (!(response instanceof TransactionResponseWithUpdates trwu) || trwu.getUpdates().count() > 1L)
					response = deliverTransaction(request);
	
				if (response instanceof MethodCallTransactionFailedResponse responseAsFailed)
					LOGGER.log(Level.WARNING, "could not reward the validators: " + responseAsFailed.getWhere() + ": " + responseAsFailed.getClassNameOfCause() + ": " + responseAsFailed.getMessageOfCause());
				else {
					LOGGER.info("units of gas consumed for CPU, RAM or storage since the previous reward: " + gasConsumed);
					LOGGER.info("units of coin rewarded to the validators for their work since the previous reward: " + coins);
					LOGGER.info("units of coin minted since the previous reward: " + minted);
				}
			}
		}
		catch (TransactionRejectedException | FieldNotFoundException | UnknownReferenceException e) {
			throw new StoreException("Could not reward the validators", e);
		}
	}

	@Override
	public final TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException {
		var reference = TransactionReferences.of(store.getHasher().hash(request));
		String referenceAsString = reference.toString();
	
		try {
			LOGGER.info(referenceAsString + ": delivering start");
	
			ResponseBuilder<?,?> responseBuilder = responseBuilderFor(reference, request);
			TransactionResponse response = responseBuilder.getResponse();
			push(reference, request, response);
			responseBuilder.replaceReverifiedResponses();
			takeNoteForNextReward(request, response);
			invalidateCachesIfNeeded(response, responseBuilder.getClassLoader());
	
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
	public final TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException {
		var request = deltaRequests.get(reference);
		return request != null ? request : getInitialStore().getRequest(reference);
	}

	@Override
	public final TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException {
		var response = deltaResponses.get(reference);
		return response != null ? response : getInitialStore().getResponse(reference);
	}

	@Override
	public final Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException, StoreException {
		var history = deltaHistories.get(object);
		return history != null ? Stream.of(history) : getInitialStore().getHistory(object);
	}

	@Override
	public final Optional<StorageReference> getManifest() throws StoreException {
		var uncommittedManifest = manifest;
		return uncommittedManifest != null ? Optional.of(uncommittedManifest) : getInitialStore().getManifest();
	}

	public final Stream<TransactionReference> getDeliveredTransactions() {
		return deltaRequests.keySet().stream();
	}

	@Override
	protected final long getNow() {
		return now;
	}

	@Override
	protected final StoreCache getCache() {
		return cache;
	}

	@Override
	protected final Hasher<TransactionRequest<?>> getHasher() {
		return store.getHasher();
	}

	/**
	 * Invalidates the caches, if needed, after the addition of the given response into store.
	 * 
	 * @param response the store
	 * @param classLoader the class loader of the transaction that computed {@code response}
	 * @throws ClassNotFoundException if some class cannot be found in the Takamaka code
	 */
	protected void invalidateCachesIfNeeded(TransactionResponse response, EngineClassLoader classLoader) throws StoreException {
		if (manifestMightHaveChanged(response)) {
			cache = cache.setValidators(extractValidators());
			LOGGER.info("the validators cache has been updated since it might have changed");
			cache = cache.setGasStation(extractGasStation());
			LOGGER.info("the gas station cache has been updated since it might have changed");
			cache = cache.setVersions(extractVersions());
			LOGGER.info("the versions manager cache has been updated since it might have changed");
		}

		if (consensusParametersMightHaveChanged(response, classLoader)) {
			long versionBefore = cache.getConfig().getVerificationVersion();
			cache = cache.setConfig(extractConsensus()).invalidateClassLoaders();
			long versionAfter = cache.getConfig().getVerificationVersion();
			LOGGER.info("the consensus parameters cache has been updated since it might have changed");

			if (versionBefore != versionAfter)
				LOGGER.info("the version of the verification module has changed from " + versionBefore + " to " + versionAfter);
		}

		if (gasPriceMightHaveChanged(response, classLoader)) {
			cache = cache.setGasPrice(extractGasPrice());
			LOGGER.info("the gas cache has been updated since it might have changed: the new gas price is " + cache.getGasPrice().get());
		}

		if (inflationMightHaveChanged(response, classLoader)) {
			cache = cache.setInflation(extractInflation());
			LOGGER.info("the inflation cache has been updated since it might have changed: the new inflation is " + cache.getInflation().getAsLong());
		}
	}

	/**
	 * Writes in store the given response for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param response the response
	 */
	protected final void setResponse(TransactionReference reference, TransactionResponse response) {
		deltaResponses.put(reference, response);
	}

	protected final StorageReference getCreator(StorageReference event) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getReferenceField(event, FieldSignatures.EVENT_CREATOR_FIELD);
	}

	/**
	 * Writes in store the given request for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param request the request
	 * @throws StoreException if this store is not able to complete the operation correctly
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
	 * @param history the stream that will become the history of the object,
	 *                replacing its previous history; this is in chronological order,
	 *                from newest transactions to oldest; hence the last transaction is
	 *                that when the object has been created
	 */
	private void setHistory(StorageReference object, Stream<TransactionReference> history) {
		deltaHistories.put(object, history.toArray(TransactionReference[]::new));
	}

	/**
	 * Mark the node as initialized. This happens for initialization requests.
	 * 
	 * @param manifest the manifest to put in the node
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	private void setManifest(StorageReference manifest) {
		this.manifest = manifest;
	}

	/*
	 * Determines if the given response might change the value of some consensus parameter.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the value of some consensus parameters; otherwise,
	 *         it is more efficient to return false, since true might trigger a recomputation
	 *         of the consensus parameters' cache
	 */
	private boolean consensusParametersMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) throws StoreException {
		if (response instanceof InitializationTransactionResponse)
			return true;
		// we check if there are events of type ConsensusUpdate triggered by the manifest, validators, gas station or versions
		else if (response instanceof TransactionResponseWithEvents trwe && trwe.getEvents().findAny().isPresent()) {
			Optional<StorageReference> maybeManifest = getManifest();
	
			if (maybeManifest.isPresent()) {
				var manifest = maybeManifest.get();
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				StorageReference versions = getVersions().orElseThrow(() -> new StoreException("The manifest is set but the versions are not set"));
				StorageReference gasStation = getGasStation().orElseThrow(() -> new StoreException("The manifest is set but gas station is not set"));
				Stream<StorageReference> events = trwe.getEvents();
	
				try {
					return check(StoreException.class, UnknownReferenceException.class, FieldNotFoundException.class, () ->
						events.filter(uncheck(event -> isConsensusUpdateEvent(event, classLoader)))
						.map(UncheckFunction.uncheck(this::getCreator))
						.anyMatch(creator -> creator.equals(manifest) || creator.equals(validators) || creator.equals(gasStation) || creator.equals(versions)));
				}
				catch (UnknownReferenceException | FieldNotFoundException e) {
					// if it was possible to verify that it is an event, then it exists in store and must have a creator or otherwise the store is corrupted
					throw new StoreException(e);
				}
			}
		}
	
		return false;
	}

	private boolean isConsensusUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws StoreException {
		try {
			return classLoader.isConsensusUpdateEvent(getClassName(event));
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Event " + event + " is not an object in store", e);
		}
		catch (ClassNotFoundException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Determines if the given response might change the gas price.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the gas price
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	private boolean gasPriceMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) throws StoreException {
		if (response instanceof InitializationTransactionResponse)
			return true;
		else if (response instanceof TransactionResponseWithEvents trwe && trwe.getEvents().findAny().isPresent()) {
			Optional<StorageReference> maybeGasStation = getGasStation();
	
			if (maybeGasStation.isPresent()) {
				var gasStation = maybeGasStation.get();
				Stream<StorageReference> events = trwe.getEvents();
	
				try {
					return check(StoreException.class, UnknownReferenceException.class, FieldNotFoundException.class, () ->
						events.filter(uncheck(event -> isGasPriceUpdateEvent(event, classLoader)))
						.map(UncheckFunction.uncheck(this::getCreator))
						.anyMatch(gasStation::equals));
				}
				catch (UnknownReferenceException | FieldNotFoundException e) {
					// if it was possible to verify that it is an event, then it exists in store and must have a creator or otherwise the store is corrupted
					throw new StoreException(e);
				}
			}
		}
	
		return false;
	}

	private boolean isGasPriceUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws StoreException {
		try {
			return classLoader.isGasPriceUpdateEvent(getClassName(event));
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Event " + event + " is not an object in store", e);
		}
		catch (ClassNotFoundException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Determines if the given response might change the current inflation.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the current inflation
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	private boolean inflationMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) throws StoreException {
		if (response instanceof InitializationTransactionResponse)
			return true;
		else if (response instanceof TransactionResponseWithEvents trwe && trwe.getEvents().findAny().isPresent()) {
			Optional<StorageReference> maybeValidators = getValidators();
	
			if (maybeValidators.isPresent()) {
				var validators = maybeValidators.get();
				Stream<StorageReference> events = trwe.getEvents();
	
				try {
					return check(StoreException.class, UnknownReferenceException.class, FieldNotFoundException.class, () ->
						events.filter(uncheck(event -> isInflationUpdateEvent(event, classLoader)))
						.map(UncheckFunction.uncheck(this::getCreator))
						.anyMatch(validators::equals));
				}
				catch (UnknownReferenceException | FieldNotFoundException e) {
					// if it was possible to verify that it is an event, then it exists in store and must have a creator or otherwise the store is corrupted
					throw new StoreException(e);
				}
			}
		}
	
		return false;
	}

	private boolean isInflationUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws StoreException {
		try {
			return classLoader.isInflationUpdateEvent(getClassName(event));
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Event " + event + " is not an object in store", e);
		}
		catch (ClassNotFoundException e) {
			throw new StoreException(e);
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
	 * @throws StoreException if the operation could not be successfully completed
	 */
	private void takeNoteForNextReward(TransactionRequest<?> request, TransactionResponse response) throws StoreException {
		if (!(request instanceof SystemTransactionRequest) && response instanceof NonInitialTransactionResponse responseAsNonInitial) {
			BigInteger gasConsumedButPenalty = responseAsNonInitial.getGasConsumedForCPU()
				.add(responseAsNonInitial.getGasConsumedForStorage())
				.add(responseAsNonInitial.getGasConsumedForRAM());

			gasConsumed = gasConsumed.add(gasConsumedButPenalty);

			BigInteger gasConsumedTotal = gasConsumedButPenalty;
			if (response instanceof FailedTransactionResponse ftr)
				gasConsumedTotal = gasConsumedTotal.add(ftr.getGasConsumedForPenalty());

			BigInteger gasPrice = ((NonInitialTransactionRequest<?>) request).getGasPrice();
			BigInteger reward = gasConsumedTotal.multiply(gasPrice);
			coinsWithoutInflation = coinsWithoutInflation.add(reward);

			gasConsumedTotal = addInflation(gasConsumedTotal);
			reward = gasConsumedTotal.multiply(gasPrice);
			coins = coins.add(reward);
		}
	}

	private BigInteger addInflation(BigInteger gas) throws StoreException {
		OptionalLong currentInflation = cache.getInflation();
	
		if (currentInflation.isPresent())
			gas = gas.multiply(_100_000_000.add(BigInteger.valueOf(currentInflation.getAsLong())))
					 .divide(_100_000_000);
	
		return gas;
	}

	/**
	 * Pushes the result of executing a successful Hotmoka request.
	 * This method assumes that the given request was not already present in the store.
	 * This method yields a store where the push is visible. Checkable stores remain
	 * unchanged after a call to this method, while non-checkable stores might be
	 * modified and coincide with the result of the method.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 * @return the store resulting after the push
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	private void push(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws StoreException {
		if (response instanceof TransactionResponseWithUpdates trwu) {
			setRequest(reference, request);
			setResponse(reference, trwu);
			expandHistory(reference, trwu);
	
			if (response instanceof GameteCreationTransactionResponse gctr)
				LOGGER.info(gctr.getGamete() + ": created as gamete");
		}
		else if (response instanceof InitializationTransactionResponse) {
			if (request instanceof InitializationTransactionRequest itr) {
				setRequest(reference, request);
				setResponse(reference, response);
				StorageReference manifest = itr.getManifest();
				setManifest(manifest);
				LOGGER.info(manifest + ": set as manifest");
				LOGGER.info("the node has been initialized");
			}
			else
				throw new StoreException("Trying to initialize the node with a request of class " + request.getClass().getSimpleName());
		}
		else {
			setRequest(reference, request);
			setResponse(reference, response);
		}
	}

	private BigInteger getCurrentSupply(StorageReference validators) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(validators, FieldSignatures.ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD);
	}

	/**
	 * Process the updates contained in the given response, expanding the history of the affected objects.
	 * 
	 * @param reference the transaction that has generated the given response
	 * @param response the response
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	private void expandHistory(TransactionReference reference, TransactionResponseWithUpdates response) throws StoreException {
		// we collect the storage references that have been updated in the response; for each of them,
		// we fetch the list of the transaction references that affected them in the past, we add the new transaction reference
		// in front of such lists and store back the updated lists, replacing the old ones
		CheckRunnable.check(StoreException.class, () ->
			response.getUpdates()
				.map(Update::getObject)
				.distinct()
				.forEachOrdered(UncheckConsumer.uncheck(object -> setHistory(object, simplifiedHistory(object, reference, response.getUpdates())))));
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
	private Stream<TransactionReference> simplifiedHistory(StorageReference objectUpdatedInResponse, TransactionReference added, Stream<Update> addedUpdates) throws StoreException {
		// if the object has been created at the added transaction, that is its history
		if (objectUpdatedInResponse.getTransaction().equals(added))
			return Stream.of(added);

		Stream<TransactionReference> old;

		try {
			old = getHistory(objectUpdatedInResponse);
		}
		catch (UnknownReferenceException e) {
			// the object was created before this transaction: it must have a history or otherwise the store is corrupted
			throw new StoreException("The computed response reports a modified object that is not in store", e);
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
	
		return simplified.stream();
	}

	/**
	 * Adds the given transaction reference to the history of the given object,
	 * if it provides updates for fields that have not yet been covered by other updates.
	 * 
	 * @param referenceInHistory the transaction reference
	 * @param object the object
	 * @param covered the set of updates for the already covered fields
	 * @param history the history; this might be modified by the method, by prefixing {@code reference} at its front
	 */
	private void addIfUncovered(TransactionReference referenceInHistory, StorageReference object, Set<Update> covered, List<TransactionReference> history) throws StoreException {
		if (getUpdates(referenceInHistory).filter(update -> update.getObject().equals(object) && covered.stream().noneMatch(update::sameProperty) && covered.add(update)).count() > 0)
			history.add(referenceInHistory);
	}
}