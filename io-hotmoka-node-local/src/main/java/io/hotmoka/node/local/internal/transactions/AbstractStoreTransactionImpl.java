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

package io.hotmoka.node.local.internal.transactions;

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;
import static io.hotmoka.node.MethodSignatures.GET_CURRENT_INFLATION;
import static io.hotmoka.node.MethodSignatures.GET_GAS_PRICE;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.CheckRunnable;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckConsumer;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.AbstractInstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.NonInitialTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SystemTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.FailedTransactionResponse;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.ResponseBuilder;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.StoreTransaction;

/**
 * The store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. A store is external to the node and, typically, only
 * its hash is held in the node, if consensus is needed. Stores must be thread-safe, since they can
 * be used concurrently for executing more requests.
 */
public abstract class AbstractStoreTransactionImpl<S extends AbstractStoreImpl<S, T>, T extends AbstractStoreTransactionImpl<S, T>> implements StoreTransaction<S, T> {
	private final static Logger LOGGER = Logger.getLogger(AbstractStoreTransactionImpl.class.getName());
	private final S store;

	private final ConcurrentMap<TransactionReference, TransactionRequest<?>> requests = new ConcurrentHashMap<>();
	private final ConcurrentMap<TransactionReference, TransactionResponse> responses = new ConcurrentHashMap<>();

	/**
	 * The histories of the objects created in blockchain. In a real implementation, this must
	 * be stored in a persistent state.
	 */
	private final ConcurrentMap<StorageReference, TransactionReference[]> histories = new ConcurrentHashMap<>();

	/**
	 * The errors generated by each transaction (if any). In a real implementation, this must
	 * be stored in a persistent memory such as a blockchain.
	 */
	private final ConcurrentMap<TransactionReference, String> errors = new ConcurrentHashMap<>();

	/**
	 * The storage reference of the manifest stored inside the node, if any.
	 */
	private volatile StorageReference manifest;

	private volatile LRUCache<TransactionReference, Boolean> checkedSignatures;

	private volatile LRUCache<TransactionReference, EngineClassLoader> classLoaders;

	/**
	 * The current gas price in this store transaction. This information could be recovered from the store
	 * transaction itself, but this field is used for caching. The gas price might be missing if the
	 * node is not initialized yet.
	 */
	private volatile Optional<BigInteger> gasPrice;

	/**
	 * The current inflation in this store transaction. This information could be recovered from the store
	 * transaction itself, but this field is used for caching. The inflation might be missing if the
	 * node is not initialized yet.
	 */
	private volatile OptionalLong inflation;

	/**
	 * The current consensus configuration in this store transaction. This information could be recovered from
	 * the store transaction itself, but this field is used for caching. This information might be
	 * missing after a store check out to a specific root, after which the cache has not been recomputed yet.
	 */
	private volatile ConsensusConfig<?,?> consensus;

	/**
	 * The gas consumed for CPU execution, RAM or storage in this transaction.
	 */
	private volatile BigInteger gasConsumed;

	/**
	 * The reward to send to the validators, accumulated during this transaction.
	 */
	private volatile BigInteger coins;

	/**
	 * The reward to send to the validators, accumulated during this transaction, without considering the inflation.
	 */
	private volatile BigInteger coinsWithoutInflation;

	/**
	 * The number of Hotmoka requests executed during this transaction.
	 */
	private volatile BigInteger numberOfRequests;

	/**
	 * The transactions containing events that must be notified at commit-time.
	 */
	private final Set<TransactionResponseWithEvents> responsesWithEventsToNotify = ConcurrentHashMap.newKeySet();

	private final Hasher<TransactionRequest<?>> hasher;

	private final long now;

	private final ExecutorService executors;

	/**
	 * Enough gas for a simple get method.
	 */
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	private final static BigInteger _100_000_000 = BigInteger.valueOf(100_000_000L);

	protected AbstractStoreTransactionImpl(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) {
		this.store = store;
		this.executors = executors;
		this.now = now;
		this.checkedSignatures = store.checkedSignatures; //new LRUCache<>(store.checkedSignatures);
		this.classLoaders = store.classLoaders; //new LRUCache<>(store.classLoaders); // TODO: clone?
		this.gasPrice = store.gasPrice;
		this.inflation = store.inflation;
		this.consensus = consensus;
		this.hasher = store.hasher;
		this.gasConsumed = BigInteger.ZERO;
		this.coins = BigInteger.ZERO;
		this.coinsWithoutInflation = BigInteger.ZERO;
		this.numberOfRequests = BigInteger.ZERO;
	}

	@Override
	public final S getInitialStore() {
		return store;
	}

	@Override
	public final S getFinalStore() throws StoreException {
		return mkFinalStore(checkedSignatures, classLoaders, consensus, gasPrice, inflation, requests, responses, histories, errors, Optional.ofNullable(manifest));
	}

	protected abstract S mkFinalStore(LRUCache<TransactionReference, Boolean> checkedSignatures, LRUCache<TransactionReference, EngineClassLoader> classLoaders,
    		ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation, Map<TransactionReference, TransactionRequest<?>> addedRequests,
    		Map<TransactionReference, TransactionResponse> addedResponses,
    		Map<StorageReference, TransactionReference[]> addedHistories,
    		Map<TransactionReference, String> addedErrors,
    		Optional<StorageReference> addedManifest) throws StoreException;

	@Override
	public final long getNow() {
		return now;
	}

	@Override
	public final ConsensusConfig<?,?> getConfig() {
		return consensus;
	}

	@Override
	public void invalidateConsensusCache() throws StoreException {
		LOGGER.info("the consensus parameters have been reset");
		recomputeConsensus();
	}

	@Override
	public final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return getOutcome(new InstanceViewMethodCallResponseBuilder(reference, request, this).getResponse());
	}

	@Override
	public final Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return getOutcome(new StaticViewMethodCallResponseBuilder(reference, request, this).getResponse());
	}

	@Override
	public void rewardValidators(String behaving, String misbehaving) throws StoreException {
		try {
			Optional<StorageReference> maybeManifest = getManifest();
			if (maybeManifest.isPresent()) {
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
					StorageValues.bigIntegerOf(gasConsumed), StorageValues.bigIntegerOf(numberOfRequests));
	
				ResponseBuilder<?,?> responseBuilder = responseBuilderFor(TransactionReferences.of(hasher.hash(request)), request);
				TransactionResponse response = responseBuilder.getResponse();
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
			LOGGER.log(Level.WARNING, "could not reward the validators", e);
			throw new StoreException("Could not reward the validators", e);
		}
	}

	@Override
	public ResponseBuilder<?,?> responseBuilderFor(TransactionReference reference, TransactionRequest<?> request) throws TransactionRejectedException {
		if (request instanceof JarStoreInitialTransactionRequest jsitr)
			return new JarStoreInitialResponseBuilder(reference, jsitr, this);
		else if (request instanceof GameteCreationTransactionRequest gctr)
			return new GameteCreationResponseBuilder(reference, gctr, this);
		else if (request instanceof JarStoreTransactionRequest jstr)
			return new JarStoreResponseBuilder(reference, jstr, this);
		else if (request instanceof ConstructorCallTransactionRequest cctr)
			return new ConstructorCallResponseBuilder(reference, cctr, this);
		else if (request instanceof AbstractInstanceMethodCallTransactionRequest aimctr)
			return new InstanceMethodCallResponseBuilder(reference, aimctr, this);
		else if (request instanceof StaticMethodCallTransactionRequest smctr)
			return new StaticMethodCallResponseBuilder(reference, smctr, this);
		else if (request instanceof InitializationTransactionRequest itr)
			return new InitializationResponseBuilder(reference, itr, this);
		else
			throw new TransactionRejectedException("Unexpected transaction request of class " + request.getClass().getName());
	}

	@Override
	public final TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException {
		var reference = TransactionReferences.of(hasher.hash(request));
	
		try {
			LOGGER.info(reference + ": delivering start (" + request.getClass().getSimpleName() + ')');
	
			ResponseBuilder<?,?> responseBuilder = responseBuilderFor(reference, request);
			TransactionResponse response = responseBuilder.getResponse();
			push(reference, request, response);
			responseBuilder.replaceReverifiedResponses();
			scheduleEventsForNotificationAfterCommit(response);
			takeNoteForNextReward(request, response);
			invalidateCachesIfNeeded(response, responseBuilder.getClassLoader());
	
			LOGGER.info(reference + ": delivering success");
			return response;
		}
		catch (TransactionRejectedException e) {
			push(reference, request, trimmedMessage(e));
			LOGGER.info(reference + ": delivering failed: " + trimmedMessage(e));
			throw e;
		}
		catch (NodeException | UnknownReferenceException e) { // TODO: these should disappear
			LOGGER.log(Level.SEVERE, reference + ": delivering failed with unexpected exception", e);
			throw new StoreException(e);
		}
	}

	@Override
	public final void notifyAllEvents(BiConsumer<StorageReference, StorageReference> notifier) throws StoreException {
		try {
			CheckRunnable.check(StoreException.class, UnknownReferenceException.class, FieldNotFoundException.class, () ->
				responsesWithEventsToNotify.stream()
					.flatMap(TransactionResponseWithEvents::getEvents)
					.forEachOrdered(UncheckConsumer.uncheck(event -> notifier.accept(getCreator(event), event))));
		}
		catch (UnknownReferenceException | FieldNotFoundException e) {
			// the set of events to notify contains an event that cannot be found in store or that
			// has no creator field: the delivery method of the store is definitely misbehaving
			throw new StoreException(e);
		}
	}

	/**
	 * Yields the response of the transaction having the given reference.
	 * This considers also updates inside this transaction, that have not yet been committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 */
	protected final TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException {
		var uncommittedResponse = responses.get(reference);
		return uncommittedResponse != null ? uncommittedResponse : getInitialStore().getResponse(reference);
	}

	/**
	 * Yields the history of the given object, that is, the references to the transactions
	 * that can be used to reconstruct the current values of its fields.
	 * This considers also updates inside this transaction, that have not yet been committed.
	 * 
	 * @param object the reference of the object
	 * @return the history. Yields an empty stream if there is no history for {@code object}
	 * @throws StoreException if the store is not able to perform the operation
	 */
	protected final Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException, StoreException {
		var uncommittedHistory = histories.get(object);
		return uncommittedHistory != null ? Stream.of(uncommittedHistory) : getInitialStore().getHistory(object);
	}

	/**
	 * Yields the manifest installed when the node is initialized.
	 * This considers also updates inside this transaction, that have not yet been committed.
	 * 
	 * @return the manifest
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	protected final Optional<StorageReference> getManifest() throws StoreException {
		var uncommittedManifest = manifest;
		return uncommittedManifest != null ? Optional.of(uncommittedManifest) : getInitialStore().getManifest();
	}

	/**
	 * Writes in store the given request for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param request the request
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected final void setRequest(TransactionReference reference, TransactionRequest<?> request) {
		requests.put(reference, request);
	}

	/**
	 * Writes in store the given response for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param response the response
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected final void setResponse(TransactionReference reference, TransactionResponse response) {
		responses.put(reference, response);
	}

	/**
	 * Writes in store the given error for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param error the error
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected final void setError(TransactionReference reference, String error) {
		errors.put(reference, error);
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
	protected final void setHistory(StorageReference object, Stream<TransactionReference> history) {
		histories.put(object, history.toArray(TransactionReference[]::new));
	}

	/**
	 * Mark the node as initialized. This happens for initialization requests.
	 * 
	 * @param manifest the manifest to put in the node
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected final void setManifest(StorageReference manifest) {
		this.manifest = manifest;
	}

	/**
	 * Yields the current gas price at the end of this transaction.
	 * This might be missing if the node is not initialized yet.
	 * 
	 * @return the current gas price at the end of this transaction
	 */
	protected final Optional<BigInteger> getGasPrice() throws StoreException {
		if (gasPrice.isEmpty())
			recomputeGasPrice();
	
		return gasPrice;
	}

	private void recomputeGasPrice() throws StoreException {
		try {
			Optional<StorageReference> manifest = getManifest();
			if (manifest.isPresent()) {
				TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
				StorageReference gasStation = getGasStation().orElseThrow(() -> new StoreException("The manifest is set but the gas station is not set"));
				StorageValue result = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest.get(), _100_000, takamakaCode, GET_GAS_PRICE, gasStation))
					.orElseThrow(() -> new StoreException(GET_GAS_PRICE + " should not return void"));

				if (result instanceof BigIntegerValue biv) {
					BigInteger newGasPrice = biv.getValue();
					gasPrice = Optional.of(newGasPrice);
					LOGGER.info("the gas price cache has been updated to " + newGasPrice);
				}
				else
					throw new StoreException(GET_GAS_PRICE + " should return a BigInteger, not a " + result.getClass().getName());
			}
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Yields the current inflation of the node.
	 * 
	 * @return the current inflation of the node, if the node is already initialized
	 */
	protected final OptionalLong getInflation() throws StoreException {
		if (inflation.isEmpty())
			recomputeInflation();

		return inflation;
	}

	private void recomputeInflation() throws StoreException {
		try {
			Optional<StorageReference> manifest = getManifest();
			if (manifest.isPresent()) {
				TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				StorageValue result = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest.get(), _100_000, takamakaCode, GET_CURRENT_INFLATION, validators))
					.orElseThrow(() -> new StoreException(GET_CURRENT_INFLATION + " should not return void"));

				if (result instanceof LongValue lv) {
					long newInflation = lv.getValue();
					inflation = OptionalLong.of(newInflation);
					LOGGER.info("the inflation cache has been updated to " + newInflation);
				}
				else
					throw new StoreException(GET_CURRENT_INFLATION + " should return a long, not a " + result.getClass().getName());
			}
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new StoreException(e);
		}
	}

	protected <X> Future<X> submit(Callable<X> task) {
		return executors.submit(task);
	}

	protected final LRUCache<TransactionReference, Boolean> getCheckedSignatures() {
		return checkedSignatures;
	}

	protected final LRUCache<TransactionReference, EngineClassLoader> getClassLoaders() {
		return classLoaders;
	}

	private void recomputeConsensus() throws StoreException {
		try {
			Optional<StorageReference> maybeManifest = getManifest();
			if (maybeManifest.isPresent()) {
				StorageReference manifest = maybeManifest.get();
				TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				StorageReference gasStation = getGasStation().orElseThrow(() -> new StoreException("The manifest is set but the gas station is not set"));
				StorageReference versions = getVersions().orElseThrow(() -> new StoreException("The manifest is set but the versions are not set"));

				String genesisTime = ((StringValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall // TODO: check casts
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_GENESIS_TIME, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_GENESIS_TIME + " should not return void"))).getValue();

				String chainId = ((StringValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_CHAIN_ID + " should not return void"))).getValue();

				StorageReference gamete = (StorageReference) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_GAMETE + " should not return void"));

				String publicKeyOfGamete = ((StringValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, gamete))
						.orElseThrow(() -> new StoreException(MethodSignatures.PUBLIC_KEY + " should not return void"))).getValue();

				int maxErrorLength = ((IntValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_ERROR_LENGTH, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_ERROR_LENGTH + " should not return void"))).getValue();

				int maxDependencies = ((IntValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_DEPENDENCIES, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_DEPENDENCIES + " should not return void"))).getValue();

				long maxCumulativeSizeOfDependencies = ((LongValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES + " should not return void"))).getValue();

				boolean allowsFaucet = ((BooleanValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_UNSIGNED_FAUCET, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.ALLOWS_UNSIGNED_FAUCET + " should not return void"))).getValue();

				boolean skipsVerification = ((BooleanValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.SKIPS_VERIFICATION, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.SKIPS_VERIFICATION + " should not return void"))).getValue();

				String signature = ((StringValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_SIGNATURE, manifest))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_SIGNATURE + " should not return void"))).getValue();

				BigInteger ticketForNewPoll = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_TICKET_FOR_NEW_POLL, validators))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_TICKET_FOR_NEW_POLL + " should not return void"))).getValue();

				BigInteger initialGasPrice = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_GAS_PRICE, gasStation))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_GAS_PRICE + " should not return void"))).getValue();

				BigInteger maxGasPerTransaction = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, gasStation))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_GAS_PER_TRANSACTION + " should not return void"))).getValue();

				boolean ignoresGasPrice = ((BooleanValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.IGNORES_GAS_PRICE, gasStation))
						.orElseThrow(() -> new StoreException(MethodSignatures.IGNORES_GAS_PRICE + " should not return void"))).getValue();

				BigInteger targetGasAtReward = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_TARGET_GAS_AT_REWARD, gasStation))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_TARGET_GAS_AT_REWARD + " should not return void"))).getValue();

				long oblivion = ((LongValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_OBLIVION, gasStation))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_OBLIVION + " should not return void"))).getValue();

				long initialInflation = ((LongValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_INFLATION, validators))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_INFLATION + " should not return void"))).getValue();

				long verificationVersion = ((LongValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERIFICATION_VERSION, versions))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_VERIFICATION_VERSION + " should not return void"))).getValue();

				BigInteger initialSupply = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_SUPPLY, validators))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_SUPPLY + " should not return void"))).getValue();

				BigInteger initialRedSupply = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_RED_SUPPLY, validators))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_RED_SUPPLY + " should not return void"))).getValue();

				BigInteger finalSupply = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.GET_FINAL_SUPPLY, validators))
						.orElseThrow(() -> new StoreException(MethodSignatures.GET_FINAL_SUPPLY + " should not return void"))).getValue();

				var method1 = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getBuyerSurcharge", StorageTypes.INT);
				int buyerSurcharge = ((IntValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, method1, validators))
						.orElseThrow(() -> new StoreException(method1 + " should not return void"))).getValue();

				var method2 = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getSlashingForMisbehaving", StorageTypes.INT);
				int slashingForMisbehaving = ((IntValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, method2, validators))
						.orElseThrow(() -> new StoreException(method2 + " should not return void"))).getValue();

				var method3 = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getSlashingForNotBehaving", StorageTypes.INT);
				int slashingForNotBehaving = ((IntValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, method3, validators))
						.orElseThrow(() -> new StoreException(method3 + " should not return void"))).getValue();

				var method4 = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getPercentStaked", StorageTypes.INT);
				int percentStaked = ((IntValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, method4, validators))
						.orElseThrow(() -> new StoreException(method4 + " should not return void"))).getValue();

				var signatureAlgorithm = SignatureAlgorithms.of(signature);

				consensus = ValidatorsConsensusConfigBuilders.defaults()
						.setGenesisTime(LocalDateTime.parse(genesisTime, DateTimeFormatter.ISO_DATE_TIME))
						.setChainId(chainId)
						.setMaxGasPerTransaction(maxGasPerTransaction)
						.ignoreGasPrice(ignoresGasPrice)
						.setSignatureForRequests(signatureAlgorithm)
						.setInitialGasPrice(initialGasPrice)
						.setTargetGasAtReward(targetGasAtReward)
						.setOblivion(oblivion)
						.setInitialInflation(initialInflation)
						.setMaxErrorLength(maxErrorLength)
						.setMaxDependencies(maxDependencies)
						.setMaxCumulativeSizeOfDependencies(maxCumulativeSizeOfDependencies)
						.allowUnsignedFaucet(allowsFaucet)
						.skipVerification(skipsVerification)
						.setVerificationVersion(verificationVersion)
						.setTicketForNewPoll(ticketForNewPoll)
						.setInitialSupply(initialSupply)
						.setFinalSupply(finalSupply)
						.setInitialRedSupply(initialRedSupply)
						.setPublicKeyOfGamete(signatureAlgorithm.publicKeyFromEncoding(Base64.fromBase64String(publicKeyOfGamete)))
						.setPercentStaked(percentStaked)
						.setBuyerSurcharge(buyerSurcharge)
						.setSlashingForMisbehaving(slashingForMisbehaving)
						.setSlashingForNotBehaving(slashingForNotBehaving)
						.build();
			}
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | Base64ConversionException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Invalidates the caches, if needed, after the addition of the given response into store.
	 * 
	 * @param response the store
	 * @param classLoader the class loader of the transaction that computed {@code response}
	 * @throws ClassNotFoundException if some class cannot be found in the Takamaka code
	 */
	protected void invalidateCachesIfNeeded(TransactionResponse response, EngineClassLoader classLoader) throws StoreException {
		if (consensusParametersMightHaveChanged(response, classLoader)) {
			LOGGER.info("the consensus parameters might have changed: recomputing their cache");
			long versionBefore = consensus.getVerificationVersion();
			recomputeConsensus();
			classLoaders = new LRUCache<>(100, 1000);

			if (versionBefore != consensus.getVerificationVersion())
				LOGGER.info("the version of the verification module has changed from " + versionBefore + " to " + consensus.getVerificationVersion());
		}

		if (gasPriceMightHaveChanged(response, classLoader)) {
			LOGGER.info("the gas price might have changed: deleting its cache");
			gasPrice = Optional.empty();
		}

		if (inflationMightHaveChanged(response, classLoader)) {
			LOGGER.info("the inflation might have changed: deleting its cache");
			inflation = OptionalLong.empty();
		}
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

	protected final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return runInstanceMethodCallTransaction(request, TransactionReferences.of(hasher.hash(request)));
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
	 * Takes note that a new transaction has been delivered. This transaction is not a {@code @@View} transaction.
	 * 
	 * @param request the request of the transaction
	 * @param response the response computed for {@code request}
	 * @throws StoreException if the operation could not be successfully completed
	 */
	private void takeNoteForNextReward(TransactionRequest<?> request, TransactionResponse response) throws StoreException {
		if (!(request instanceof SystemTransactionRequest)) {
			numberOfRequests = numberOfRequests.add(ONE);

			if (response instanceof NonInitialTransactionResponse responseAsNonInitial) {
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
	}

	private BigInteger addInflation(BigInteger gas) throws StoreException {
		OptionalLong currentInflation = getInflation();

		if (currentInflation.isPresent())
			gas = gas.multiply(_100_000_000.add(BigInteger.valueOf(currentInflation.getAsLong())))
					 .divide(_100_000_000);

		return gas;
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
		int maxErrorLength = consensus.getMaxErrorLength();
		return length <= maxErrorLength ? message : (message.substring(0, maxErrorLength) + "...");
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

	/**
	 * Pushes into the store the error message resulting from the unsuccessful execution of a Hotmoka request.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param errorMessage the error message
	 * @return the store resulting after the push
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	private void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) throws StoreException {
		setRequest(reference, request);
		setError(reference, errorMessage);
	}

	protected final Optional<TransactionReference> getTakamakaCode() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isEmpty())
			return Optional.empty();

		try {
			return Optional.of(getClassTag(maybeManifest.get()).getJar());
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("The manifest is set to something that is not an object", e);
		}
	}

	private Optional<StorageReference> getGasStation() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isPresent()) {
			try {
				return Optional.of(getReferenceField(maybeManifest.get(), FieldSignatures.MANIFEST_GAS_STATION_FIELD));
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the gas station", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			return Optional.empty();
	}

	protected final Optional<StorageReference> getValidators() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isPresent()) {
			try {
				return Optional.of(getReferenceField(maybeManifest.get(), FieldSignatures.MANIFEST_VALIDATORS_FIELD));
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the validators set", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			return Optional.empty();
	}

	protected final Optional<StorageReference> getGamete() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isPresent()) {
			try {
				return Optional.of(getReferenceField(maybeManifest.get(), FieldSignatures.MANIFEST_GAMETE_FIELD));
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the gamete", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			return Optional.empty();
	}

	private Optional<StorageReference> getVersions() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isPresent()) {
			try {
				return Optional.of(getReferenceField(maybeManifest.get(), FieldSignatures.MANIFEST_VERSIONS_FIELD));
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the versions manager", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			return Optional.empty();
	}

	private BigInteger getBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(contract, FieldSignatures.BALANCE_FIELD);
	}

	private BigInteger getRedBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(contract, FieldSignatures.RED_BALANCE_FIELD);
	}

	private BigInteger getCurrentSupply(StorageReference validators) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(validators, FieldSignatures.ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD);
	}

	protected final String getPublicKey(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getStringField(account, FieldSignatures.EOA_PUBLIC_KEY_FIELD);
	}

	protected final StorageReference getCreator(StorageReference event) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getReferenceField(event, FieldSignatures.EVENT_CREATOR_FIELD);
	}

	protected final BigInteger getNonce(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(account, FieldSignatures.EOA_NONCE_FIELD);
	}

	protected final BigInteger getTotalBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBalance(contract).add(getRedBalance(contract));
	}

	protected final String getClassName(StorageReference reference) throws UnknownReferenceException, StoreException {
		return getClassTag(reference).getClazz().getName();
	}

	protected final ClassTag getClassTag(StorageReference reference) throws UnknownReferenceException, StoreException {
		// we go straight to the transaction that created the object
		if (getResponse(reference.getTransaction()) instanceof TransactionResponseWithUpdates trwu) {
			return trwu.getUpdates().filter(update -> update instanceof ClassTag && update.getObject().equals(reference))
					.map(update -> (ClassTag) update)
					.findFirst()
					.orElseThrow(() -> new UnknownReferenceException("Object " + reference + " does not exist"));
		}
		else
			throw new UnknownReferenceException("Transaction reference " + reference + " does not contain updates");
	}

	protected final Stream<UpdateOfField> getEagerFields(StorageReference object) throws UnknownReferenceException, StoreException {
		var fieldsAlreadySeen = new HashSet<FieldSignature>();

		return getHistory(object)
				.flatMap(CheckSupplier.check(StoreException.class, () -> UncheckFunction.uncheck(this::getUpdates)))
				.filter(update -> update.isEager() && update instanceof UpdateOfField uof && update.getObject().equals(object) && fieldsAlreadySeen.add(uof.getField()))
				.map(update -> (UpdateOfField) update);
	}

	protected final UpdateOfField getLastUpdateToField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		Stream<TransactionReference> history = getHistory(object);

		try {
			return CheckSupplier.check(StoreException.class, UnknownReferenceException.class, () -> history.map(UncheckFunction.uncheck(transaction -> getLastUpdate(object, field, transaction)))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findFirst())
					.orElseThrow(() -> new FieldNotFoundException(field));
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Object " + object + " has a history containing a reference not in store");
		}
	}

	protected final UpdateOfField getLastUpdateToFinalField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		// accesses directly the transaction that created the object
		return getLastUpdate(object, field, object.getTransaction()).orElseThrow(() -> new FieldNotFoundException(field));
	}

	private void scheduleEventsForNotificationAfterCommit(TransactionResponse response) {
		if (response instanceof TransactionResponseWithEvents responseWithEvents && responseWithEvents.getEvents().findAny().isPresent())
			responsesWithEventsToNotify.add(responseWithEvents);
	}

	protected final boolean signatureIsValid(SignedTransactionRequest<?> request, SignatureAlgorithm signatureAlgorithm) throws StoreException, UnknownReferenceException, FieldNotFoundException {
		var reference = TransactionReferences.of(hasher.hash(request));
		return CheckSupplier.check(StoreException.class, UnknownReferenceException.class, FieldNotFoundException.class, () ->
			checkedSignatures.computeIfAbsentNoException(reference, UncheckFunction.uncheck(_reference -> verifySignature(signatureAlgorithm, request))));
	}

	/**
	 * Yields a class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	protected final EngineClassLoader getClassLoader(TransactionReference classpath, ConsensusConfig<?,?> consensus) throws StoreException {
		try {
			var classLoader = classLoaders.get(classpath);
			if (classLoader != null)
				return classLoader;

			var classLoader2 = new EngineClassLoaderImpl(null, Stream.of(classpath), this, consensus);
			return classLoaders.computeIfAbsent(classpath, _classpath -> classLoader2);
		}
		catch (ClassNotFoundException e) {
			// since the class loader is created from transactions that are already in the store,
			// they should be consistent and never miss a dependent class
			throw new StoreException(e);
		}
	}

	private boolean verifySignature(SignatureAlgorithm signature, SignedTransactionRequest<?> request) throws StoreException, UnknownReferenceException, FieldNotFoundException {
		try {
			return signature.getVerifier(getPublicKey(request.getCaller(), signature), SignedTransactionRequest<?>::toByteArrayWithoutSignature).verify(request, request.getSignature());
		}
		catch (InvalidKeyException | SignatureException | Base64ConversionException | InvalidKeySpecException e) {
			LOGGER.info("the public key of " + request.getCaller() + " could not be verified: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Yields the public key of the given externally owned account.
	 * 
	 * @param reference the account
	 * @param signatureAlgorithm the signing algorithm used for the request
	 * @return the public key
	 * @throws Base64ConversionException 
	 * @throws InvalidKeySpecException 
	 * @throws StoreException 
	 * @throws FieldNotFoundException 
	 * @throws UnknownReferenceException 
	 */
	private PublicKey getPublicKey(StorageReference reference, SignatureAlgorithm signatureAlgorithm) throws Base64ConversionException, InvalidKeySpecException, UnknownReferenceException, FieldNotFoundException, StoreException {
		String publicKeyEncodedBase64 = getPublicKey(reference);
		byte[] publicKeyEncoded = Base64.fromBase64String(publicKeyEncodedBase64);
		return signatureAlgorithm.publicKeyFromEncoding(publicKeyEncoded);
	}

	private Stream<Update> getUpdates(TransactionReference referenceInHistory) throws StoreException {
		try {
			if (getResponse(referenceInHistory) instanceof TransactionResponseWithUpdates trwu)
				return trwu.getUpdates();
			else
				throw new StoreException("Transaction " + referenceInHistory + " belongs to the histories but does not contain updates");
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Transaction " + referenceInHistory + " belongs to the histories but is not present in store");
		}
	}

	private StorageReference getReferenceField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof StorageReference reference)
			return reference;
		else
			throw new FieldNotFoundException(field);
	}

	private BigInteger getBigIntegerField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof BigIntegerValue biv)
			return biv.getValue();
		else
			throw new FieldNotFoundException(field);
	}

	private String getStringField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof StringValue sv)
			return sv.getValue();
		else
			throw new FieldNotFoundException(field);
	}

	/**
	 * Yields the update to the given field of the object at the given reference, generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param reference the reference to the transaction
	 * @return the update, if any. If the field of {@code object} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdate(StorageReference object, FieldSignature field, TransactionReference reference) throws UnknownReferenceException, StoreException {
		if (getResponse(reference) instanceof TransactionResponseWithUpdates trwu)
			return trwu.getUpdates()
					.filter(update -> update instanceof UpdateOfField)
					.map(update -> (UpdateOfField) update)
					.filter(update -> update.getObject().equals(object) && update.getField().equals(field))
					.findFirst();
		else
			throw new StoreException("Transaction reference " + reference + " does not contain updates");
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