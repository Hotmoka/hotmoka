package io.takamaka.code.engine.internal;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.InitializationTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithEvents;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.updates.UpdateOfStorage;
import io.hotmoka.beans.updates.UpdateOfString;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.ConsensusParams;
import io.takamaka.code.engine.AbstractLocalNode;
import io.takamaka.code.engine.EngineClassLoader;

/**
 * The caches of a local node.
 */
public class NodeCaches {
	protected final static Logger logger = LoggerFactory.getLogger(NodeCaches.class);

	private final AbstractLocalNode<?,?> node;

	/**
	 * A check on transaction references. If it throws an exception, then the transaction reference is not legal.
	 */
	private final Consumer<TransactionReference> transactionReferenceChecker;

	/**
	 * A function that yields the class tag of an object in store, possibly not yet committed.
	 */
	private final Function<StorageReference, ClassTag> getClassTagUncommitted;

	/**
	 * A function that yields the last, possibly still uncommitted update to a field of an object in store.
	 */
	private final BiFunction<StorageReference, FieldSignature, UpdateOfField> getLastUpdateToFieldUncommitted;

	/**
	 * The cache for the requests.
	 */
	private final LRUCache<TransactionReference, TransactionRequest<?>> requests;

	/**
	 * The cache for the responses.
	 */
	private final LRUCache<TransactionReference, TransactionResponse> responses;

	/**
	 * Cached error messages of requests that failed their {@link AbstractLocalNode#checkTransaction(TransactionRequest)}.
	 * This is useful to avoid polling for the outcome of recent requests whose
	 * {@link AbstractLocalNode#checkTransaction(TransactionRequest)} failed, hence never
	 * got the chance to pass to {@link AbstractLocalNode#deliverTransaction(TransactionRequest)}.
	 */
	private final LRUCache<TransactionReference, String> recentCheckTransactionErrors;

	/**
	 * Cached recent requests that have had their signature checked.
	 * This avoids repeated signature checking in {@link AbstractLocalNode#checkTransaction(TransactionRequest)}
	 * and {@link AbstractLocalNode#deliverTransaction(TransactionRequest)}.
	 */
	private final LRUCache<SignedTransactionRequest, Boolean> checkedSignatures;

	/**
	 * The cache for the class loaders.
	 */
	private final LRUCache<TransactionReference, EngineClassLoader> classLoaders = new LRUCache<>(100, 1000);

	/**
	 * The consensus parameters of the node.
	 */
	private volatile ConsensusParams consensus;

	/**
	 * The reference to the contract that manages the validators of the node.
	 * After each transaction that consumes gas, this contract receives the
	 * price of the gas, that can later be redistributed to the validators.
	 */
	private volatile StorageReference validators;

	/**
	 * The reference to the object that manages the versions of the modules of the node.
	 */
	private volatile StorageReference versions;

	/**
	 * The reference to the object that computes the cost of the gas.
	 */
	private volatile StorageReference gasStation;

	/**
	 * A cache for the current gas price. It gets reset at each reward.
	 */
	private volatile BigInteger gasPrice;

	/**
	 * Enough gas for a simple get method.
	 */
	private final static BigInteger _10_000 = BigInteger.valueOf(10_000L);

	/**
	 * Builds the caches for the given node.
	 * 
	 * @param node the node
	 * @param consensus the consensus parameters of the node
	 * @param transactionReferenceChecker a check on transaction references. If it throws an exception, then the transaction reference is not legal
	 * @param getClassTagUncommitted a function that yields the last, possibly still uncommitted update to a field of an object in store
	 * @param getLastUpdateToFieldUncommitted a function that yields the last, possibly still uncommitted update to a field of an object in store
	 */
	public NodeCaches(AbstractLocalNode<?,?> node, ConsensusParams consensus,
			Consumer<TransactionReference> transactionReferenceChecker,
			Function<StorageReference, ClassTag> getClassTagUncommitted,
			BiFunction<StorageReference, FieldSignature, UpdateOfField> getLastUpdateToFieldUncommitted) {

		this.node = node;
		this.requests = new LRUCache<>(100, node.config.requestCacheSize);
		this.responses = new LRUCache<>(100, node.config.responseCacheSize);
		this.recentCheckTransactionErrors = new LRUCache<>(100, 1000);
		this.checkedSignatures = new LRUCache<>(100, 1000);
		this.consensus = consensus;
		this.transactionReferenceChecker = transactionReferenceChecker;
		this.getClassTagUncommitted = getClassTagUncommitted;
		this.getLastUpdateToFieldUncommitted = getLastUpdateToFieldUncommitted;
	}

	/**
	 * Invalidates the information in this cache.
	 */
	public final void invalidate() {
		requests.clear();
		responses.clear();
		recentCheckTransactionErrors.clear();
		checkedSignatures.clear();
		classLoaders.clear();
		consensus = null;
		validators = null;
		versions = null;
		gasStation = null;
		gasPrice = null;
	}

	/**
	 * Invalidates the information in this cache, after the execution of a transaction with the given classloader,
	 * that yielded the given response.
	 * 
	 * @param response the response
	 * @param classLoader the classloader
	 */
	public final void invalidateIfNeeded(TransactionResponse response, EngineClassLoader classLoader) {
		if (consensusParametersMightHaveChanged(response, classLoader)) {
			int versionBefore = consensus.verificationVersion;
			logger.info("recomputing the consensus cache since the information in the manifest might have changed");
			recomputeConsensus();
			logger.info("the consensus cache has been recomputed");
			classLoaders.clear();
			if (versionBefore != consensus.verificationVersion)
				logger.info("the version of the verification module has changed from " + versionBefore + " to " + consensus.verificationVersion);
		}
	
		if (gasPriceMightHaveChanged(response, classLoader)) {
			BigInteger gasPriceBefore = gasPrice;
			logger.info("recomputing the gas price cache since it has changed");
			recomputeGasPrice();
			logger.info("the gas price cache has been recomputed and changed from " + gasPriceBefore + " to " + gasPrice);
		}
	}

	/**
	 * Reconstructs the consensus parameters from information in the manifest.
	 */
	public final void recomputeConsensus() {
		try {
			StorageReference gasStation = getGasStation().get();
			StorageReference versions = getVersions().get();
			TransactionReference takamakaCode = getTakamakaCodeUncommitted();
			StorageReference manifest = node.getStore().getManifestUncommitted().get();
	
			String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
	
			int maxErrorLength = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_ERROR_LENGTH, manifest))).value;
	
			boolean allowsSelfCharged = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.ALLOWS_SELF_CHARGED, manifest))).value;
	
			String signature = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_SIGNATURE, manifest))).value;
	
			BigInteger maxGasPerTransaction = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_MAX_GAS_PER_TRANSACTION, gasStation))).value;
	
			boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.IGNORES_GAS_PRICE, gasStation))).value;
	
			BigInteger targetGasAtReward = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_TARGET_GAS_AT_REWARD, gasStation))).value;
	
			long oblivion = ((LongValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_OBLIVION, gasStation))).value;
	
			int verificationVersion = ((IntValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_VERIFICATION_VERSION, versions))).value;
	
			consensus = new ConsensusParams.Builder()
				.setChainId(chainId)
				.setMaxGasPerTransaction(maxGasPerTransaction)
				.ignoreGasPrice(ignoresGasPrice)
				.signRequestsWith(signature)
				.setTargetGasAtReward(targetGasAtReward)
				.setOblivion(oblivion)
				.setMaxErrorLength(maxErrorLength)
				.allowSelfCharged(allowsSelfCharged)
				.setVerificationVersion(verificationVersion)
				.build();
		}
		catch (Throwable t) {
			logger.error("could not reconstruct the consensus parameters from the manifest", t);
			throw InternalFailureException.of("could not reconstruct the consensus parameters from the manifest", t);
		}
	}

	public final void recentCheckTransactionError(TransactionReference reference, String message) {
		recentCheckTransactionErrors.put(reference, message);
	}

	public final TransactionRequest<?> getRequest(TransactionReference reference) throws Exception {
		return requests.computeIfAbsent(reference, _reference -> {
			transactionReferenceChecker.accept(_reference);
			return node.getStore().getRequest(_reference)
				.orElseThrow(() -> new NoSuchElementException("unknown transaction reference " + _reference));
		});
	}

	public final TransactionResponse getResponse(TransactionReference reference) throws Exception {
		return responses.computeIfAbsent(reference, _reference -> {
			transactionReferenceChecker.accept(_reference);

			// first we check if the request passed its checkTransaction
			// but failed its deliverTransaction: in that case, the node contains
			// the error message in its store
			Optional<String> error = node.getStore().getError(_reference);
			if (error.isPresent())
				throw new TransactionRejectedException(error.get());

			// then we check if the request did not pass its checkTransaction():
			// in that case, we might have its error message in cache
			String recentError = recentCheckTransactionErrors.get(_reference);
			if (recentError != null)
				throw new TransactionRejectedException(recentError);

			// then we check if we have the response of the request in the store
			return node.getStore().getResponse(_reference)
				.orElseThrow(() -> new NoSuchElementException("unknown transaction reference " + _reference));
		});
	}

	/**
	 * Yields the class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws Exception if the class loader cannot be created
	 */
	public final EngineClassLoader getClassLoader(TransactionReference classpath) throws Exception {
		return classLoaders.computeIfAbsent(classpath, _classpath -> new EngineClassLoader(null, Stream.of(_classpath), node, true, consensus));
	}

	/**
	 * Checks that the given request is signed with the private key of its caller.
	 * 
	 * @param request the request
	 * @param signatureAlgorithm the algorithm that must have been used for signing the request
	 * @return true if and only if the signature of {@code request} is valid
	 * @throws Exception if the signature of the request could not be checked
	 */
	public final boolean signatureIsValid(SignedTransactionRequest request, SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm) throws Exception {
		return checkedSignatures.computeIfAbsent(request, _request -> signatureAlgorithm.verify(_request, getPublicKey(_request.getCaller(), signatureAlgorithm), _request.getSignature()));
	}

	/**
	 * Yields the consensus parameters of the node.
	 * 
	 * @return the consensus parameters
	 */
	public final ConsensusParams getConsensusParams() {
		return consensus;
	}

	/**
	 * Yields the reference to the contract that collects the validators of the node.
	 * After each transaction that consumes gas, the price of the gas is sent to this
	 * contract, that can later redistribute the reward to all validators.
	 * 
	 * @return the reference to the contract, if the node is already initialized
	 */
	public final Optional<StorageReference> getValidators() {
		if (validators == null)
			node.getStore().getManifestUncommitted().ifPresent
				(_manifest -> validators = ((UpdateOfStorage) getLastUpdateToFieldUncommitted.apply(_manifest, FieldSignature.MANIFEST_VALIDATORS_FIELD)).value);

		return Optional.ofNullable(validators);
	}

	/**
	 * Yields the reference to the objects that keeps track of the
	 * versions of the modules of the node.
	 * 
	 * @return the reference to the object, if the node is already initialized
	 */
	public final Optional<StorageReference> getVersions() {
		if (versions == null)
			node.getStore().getManifestUncommitted().ifPresent
				(_manifest -> versions = ((UpdateOfStorage) getLastUpdateToFieldUncommitted.apply(_manifest, FieldSignature.MANIFEST_VERSIONS_FIELD)).value);

		return Optional.ofNullable(versions);
	}

	/**
	 * Yields the reference to the contract that keeps track of the gas cost.
	 * 
	 * @return the reference to the contract, if the node is already initialized
	 */
	public final Optional<StorageReference> getGasStation() {
		if (gasStation == null)
			node.getStore().getManifestUncommitted().ifPresent
				(_manifest -> gasStation = ((UpdateOfStorage) getLastUpdateToFieldUncommitted.apply(_manifest, FieldSignature.MANIFEST_GAS_STATION_FIELD)).value);
	
		return Optional.ofNullable(gasStation);
	}

	/**
	 * Yields the current gas price of the node.
	 * 
	 * @return the current gas price of the node, if the node is already initialized
	 */
	public final Optional<BigInteger> getGasPrice() {
		if (gasPrice == null)
			recomputeGasPrice();

		return Optional.ofNullable(gasPrice);
	}

	@SuppressWarnings("resource")
	private TransactionReference getTakamakaCodeUncommitted() throws NoSuchElementException {
		return getClassTagUncommitted.apply(node.getStore().getManifestUncommitted().get()).jar;
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
			response = node.getResponse(reference.transaction);
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

	private void recomputeGasPrice() {
		Optional<StorageReference> manifest = node.getStore().getManifestUncommitted();
		if (!manifest.isEmpty())
			try {
				gasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
						(manifest.get(), _10_000, node.getTakamakaCode(), CodeSignature.GET_GAS_PRICE, getGasStation().get()))).value;
			}
			catch (Throwable t) {
				throw InternalFailureException.of("could not determine the gas price", t);
			}
	}

	/**
	 * Determines if the given response might change the value of some consensus parameters.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the value of some consensus parameters; otherwise,
	 *         it is more efficient to return false, since true might trigger a recomputation
	 *         of the consensus parameters' cache
	 */
	private boolean consensusParametersMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) {
		if (response instanceof InitializationTransactionResponse)
			return true;

		// we check if there are events of type ConsensusUpdate triggered by the manifest, validators, gas station or versions
		if (isInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
			Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
			StorageReference manifest = node.getStore().getManifestUncommitted().get();
			StorageReference gasStation = getGasStation().get();
			StorageReference versions = getVersions().get();
			StorageReference validators = getValidators().get();

			return events.filter(event -> isConsensusUpdateEvent(event, classLoader))
				.map(event -> getLastUpdateToFieldUncommitted.apply(event, FieldSignature.EVENT_CREATOR_FIELD).getValue())
				.anyMatch(creator -> creator.equals(manifest) || creator.equals(validators) || creator.equals(gasStation) || creator.equals(versions));
		}

		return false;
	}

	/**
	 * Determines if the node is initialized, that is, its manifest has been set,
	 * although possibly not yet committed.
	 * 
	 * @return true if and only if that condition holds
	 */
	private boolean isInitializedUncommitted() {
		return node.getStore().getManifestUncommitted().isPresent();
	}

	private boolean isConsensusUpdateEvent(StorageReference event, EngineClassLoader classLoader) {
		String classNameOfEvent = getClassTagUncommitted.apply(event).className;
		return classLoader.isConsensusUpdateEvent(classNameOfEvent);
	}

	/**
	 * Determines if the given response might change the gas price.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the gas price
	 */
	private boolean gasPriceMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) {
		if (response instanceof InitializationTransactionResponse)
			return true;

		// we check if there are events of type GasPriceUpdate triggered by the gas station
		if (isInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
			Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
			StorageReference gasStation = getGasStation().get();

			return events.filter(event -> isGasPriceUpdateEvent(event, classLoader))
				.map(event -> getLastUpdateToFieldUncommitted.apply(event, FieldSignature.EVENT_CREATOR_FIELD).getValue())
				.anyMatch(gasStation::equals);
		}

		return false;
	}

	private boolean isGasPriceUpdateEvent(StorageReference event, EngineClassLoader classLoader) {
		String classNameOfEvent = getClassTagUncommitted.apply(event).className;
		return classLoader.isGasPriceUpdateEvent(classNameOfEvent);
	}
}