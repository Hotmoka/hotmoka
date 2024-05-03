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

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.exceptions.UncheckSupplier;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.LongValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.NodeCache;
import io.hotmoka.node.local.api.UnsupportedVerificationVersionException;
import io.hotmoka.stores.LRUCache;
import io.hotmoka.stores.StoreException;

/**
 * An implementation of the caches of a local node.
 */
public class NodeCachesImpl implements NodeCache {
	protected final static Logger logger = Logger.getLogger(NodeCachesImpl.class.getName());

	private final AbstractLocalNodeImpl<?,?> node;

	/**
	 * The cache for the committed responses.
	 */
	private final LRUCache<TransactionReference, TransactionResponse> responses;

	/**
	 * The cache for the class loaders.
	 */
	private final LRUCache<TransactionReference, EngineClassLoader> classLoaders = new LRUCache<>(100, 1000);

	/**
	 * The consensus parameters of the node.
	 */
	private volatile ConsensusConfig<?,?> consensus;

	/**
	 * The reference to the gamete account of the node.
	 */
	private volatile Optional<StorageReference> gamete;

	/**
	 * The reference to the contract that manages the validators of the node.
	 * After each transaction that consumes gas, this contract receives the
	 * price of the gas, that can later be redistributed to the validators.
	 */
	private volatile Optional<StorageReference> validators;

	/**
	 * The reference to the object that manages the versions of the modules of the node.
	 */
	private volatile Optional<StorageReference> versions;

	/**
	 * The reference to the object that computes the cost of the gas.
	 */
	private volatile Optional<StorageReference> gasStation;

	/**
	 * A cache for the current gas price. It gets reset if it changes.
	 */
	private volatile BigInteger gasPrice;

	/**
	 * A cache for the current inflation. It gets reset if it changes.
	 */
	private volatile Long inflation;

	/**
	 * Enough gas for a simple get method.
	 */
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Builds the caches for the given node.
	 * 
	 * @param node the node
	 * @param consensus the consensus parameters of the node
	 */
	public NodeCachesImpl(AbstractLocalNodeImpl<?, ?> node, ConsensusConfig<?,?> consensus, int responseCacheSize) {
		this.node = node;
		this.responses = new LRUCache<>(100, responseCacheSize);
		this.validators = Optional.empty();
		this.versions = Optional.empty();
		this.gasStation = Optional.empty();
		this.gamete = Optional.empty();
		this.consensus = consensus;
	}

	@Override
	public final void invalidateIfNeeded(TransactionResponse response, EngineClassLoader classLoader) throws ClassNotFoundException {
		if (consensusParametersMightHaveChanged(response, classLoader)) {
			long versionBefore = consensus.getVerificationVersion();
			logger.info("recomputing the consensus cache since the information in the manifest might have changed");
			recomputeConsensus();
			logger.info("the consensus cache has been recomputed");
			classLoaders.clear();
			if (versionBefore != consensus.getVerificationVersion())
				logger.info("the version of the verification module has changed from " + versionBefore + " to " + consensus.getVerificationVersion());
		}

		if (gasPriceMightHaveChanged(response, classLoader)) {
			BigInteger gasPriceBefore = gasPrice;
			logger.info("recomputing the gas price cache since it has changed");
			recomputeGasPrice();
			logger.info("the gas price cache has been recomputed and changed from " + gasPriceBefore + " to " + gasPrice);
		}

		if (inflationMightHaveChanged(response, classLoader)) {
			Long inflationBefore = inflation;
			logger.info("recomputing the inflation cache since it has changed");
			recomputeInflation();
			logger.info("the inflation cache has been recomputed and changed from " + inflationBefore + " to " + inflation);
		}
	}

	private Optional<TransactionReference> getTakamakaCodeUncommitted() throws StoreException {
		return node.getManifestUncommitted()
			.map(node.getStoreUtilities()::getClassTagUncommitted)
			.map(ClassTag::getJar);
	}

	@Override
	public final void recomputeConsensus() {
		try {
			StorageReference gasStation = getGasStationUncommitted().get();
			StorageReference validators = getValidatorsUncommitted().get();
			StorageReference versions = getVersionsUncommitted().get();
			TransactionReference takamakaCode = getTakamakaCodeUncommitted().get();
			StorageReference manifest = node.getManifestUncommitted().get();
	
			String genesisTime = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GENESIS_TIME, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GENESIS_TIME + " should not return void"))).getValue();

			String chainId = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_CHAIN_ID + " should not return void"))).getValue();
	
			StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAMETE + " should not return void"));

			String publicKeyOfGamete = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, gamete))
				.orElseThrow(() -> new NodeException(MethodSignatures.PUBLIC_KEY + " should not return void"))).getValue();

			int maxErrorLength = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_ERROR_LENGTH, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_ERROR_LENGTH + " should not return void"))).getValue();

			int maxDependencies = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_DEPENDENCIES, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_DEPENDENCIES + " should not return void"))).getValue();

			long maxCumulativeSizeOfDependencies = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES + " should not return void"))).getValue();

			boolean allowsFaucet = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_UNSIGNED_FAUCET, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.ALLOWS_UNSIGNED_FAUCET + " should not return void"))).getValue();

			boolean skipsVerification = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.SKIPS_VERIFICATION, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.SKIPS_VERIFICATION + " should not return void"))).getValue();

			String signature = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_SIGNATURE, manifest))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_SIGNATURE + " should not return void"))).getValue();

			BigInteger ticketForNewPoll = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_TICKET_FOR_NEW_POLL, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_TICKET_FOR_NEW_POLL + " should not return void"))).getValue();

			BigInteger initialGasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_GAS_PRICE, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_INITIAL_GAS_PRICE + " should not return void"))).getValue();

			BigInteger maxGasPerTransaction = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_MAX_GAS_PER_TRANSACTION + " should not return void"))).getValue();
	
			boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.IGNORES_GAS_PRICE, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.IGNORES_GAS_PRICE + " should not return void"))).getValue();
	
			BigInteger targetGasAtReward = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_TARGET_GAS_AT_REWARD, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_TARGET_GAS_AT_REWARD + " should not return void"))).getValue();
	
			long oblivion = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_OBLIVION, gasStation))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_OBLIVION + " should not return void"))).getValue();
	
			long initialInflation = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_INFLATION, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_INITIAL_INFLATION + " should not return void"))).getValue();

			long verificationVersion = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERIFICATION_VERSION, versions))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_VERIFICATION_VERSION + " should not return void"))).getValue();

			BigInteger initialSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_SUPPLY, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_INITIAL_SUPPLY + " should not return void"))).getValue();

			BigInteger initialRedSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_RED_SUPPLY, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_INITIAL_RED_SUPPLY + " should not return void"))).getValue();

			BigInteger finalSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_FINAL_SUPPLY, validators))
				.orElseThrow(() -> new NodeException(MethodSignatures.GET_FINAL_SUPPLY + " should not return void"))).getValue();

			var method1 = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getBuyerSurcharge", StorageTypes.INT);
			int buyerSurcharge = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, method1, validators))
				.orElseThrow(() -> new NodeException(method1 + " should not return void"))).getValue();

			var method2 = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getSlashingForMisbehaving", StorageTypes.INT);
			int slashingForMisbehaving = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, method2, validators))
				.orElseThrow(() -> new NodeException(method2 + " should not return void"))).getValue();

			var method3 = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getSlashingForNotBehaving", StorageTypes.INT);
			int slashingForNotBehaving = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, method3, validators))
				.orElseThrow(() -> new NodeException(method3 + " should not return void"))).getValue();

			var method4 = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getPercentStaked", StorageTypes.INT);
			int percentStaked = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, method4, validators))
				.orElseThrow(() -> new NodeException(method4 + " should not return void"))).getValue();

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
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | NoSuchAlgorithmException | StoreException | InvalidKeyException | NodeException | InvalidKeySpecException | Base64ConversionException e) {
			logger.log(Level.SEVERE, "could not reconstruct the consensus parameters from the manifest", e);
			throw new RuntimeException("could not reconstruct the consensus parameters from the manifest", e);
		}
	}

	@Override
	public final Optional<TransactionResponse> getResponse(TransactionReference reference) {
		return responses.computeIfAbsentOptional(Objects.requireNonNull(reference), _reference -> node.getStore().getResponse(_reference));
	}

	@Override
	public final Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		return getResponse(reference).or(UncheckSupplier.uncheck(() -> node.getResponseUncommitted(reference))); // TODO: recheck
	}

	@Override
	public final EngineClassLoader getClassLoader(TransactionReference classpath) throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException, NoSuchElementException, UnknownReferenceException, NodeException {
		var classLoader = classLoaders.get(classpath);
		if (classLoader != null)
			return classLoader;

		var classLoader2 = new EngineClassLoaderImpl(null, Stream.of(classpath), node, consensus);
		return classLoaders.computeIfAbsent(classpath, _classpath -> classLoader2);
	}

	@Override
	public final ConsensusConfig<?,?> getConsensusParams() {
		return consensus;
	}

	@Override
	public final Optional<StorageReference> getGamete() throws NodeException {
		try {
			if (gamete.isEmpty())
				gamete = node.getStoreUtilities().getGameteUncommitted();

			return gamete;
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public final Optional<StorageReference> getValidatorsUncommitted() throws NodeException {
		try {
			if (validators.isEmpty())
				validators = node.getStoreUtilities().getValidatorsUncommitted();

			return validators;
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public final Optional<StorageReference> getVersionsUncommitted() throws NodeException {
		try {
			if (versions.isEmpty())
				versions = node.getStoreUtilities().getVersionsUncommitted();

			return versions;
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public final Optional<StorageReference> getGasStationUncommitted() throws NodeException {
		try {
			if (gasStation.isEmpty())
				gasStation = node.getStoreUtilities().getGasStationUncommitted();

			return gasStation;
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	public final Optional<BigInteger> getGasPrice() {
		if (gasPrice == null)
			recomputeGasPrice();

		return Optional.ofNullable(gasPrice);
	}

	@Override
	public final Optional<Long> getCurrentInflation() {
		if (inflation == null)
			recomputeInflation();

		return Optional.ofNullable(inflation);
	}

	private void recomputeGasPrice() {
		try {
			Optional<StorageReference> manifest = node.getManifestUncommitted();
			if (manifest.isPresent())
				gasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest.get(), _100_000, getTakamakaCodeUncommitted().get(),
					MethodSignatures.GET_GAS_PRICE, getGasStationUncommitted().get()))
					.orElseThrow(() -> new NodeException(MethodSignatures.GET_GAS_PRICE + " should not return void"))).getValue();
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | StoreException | NodeException e) {
			throw new RuntimeException("could not determine the gas price", e);
		}
	}

	private void recomputeInflation() {
		try {
			Optional<StorageReference> manifest = node.getManifestUncommitted();
			if (manifest.isPresent())
				inflation = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest.get(), _100_000, getTakamakaCodeUncommitted().get(),
					MethodSignatures.GET_CURRENT_INFLATION, getValidatorsUncommitted().get()))
					.orElseThrow(() -> new NodeException(MethodSignatures.GET_CURRENT_INFLATION + " should not return void"))).getValue();
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | StoreException | NodeException e) {
			throw new RuntimeException("could not determine the current inflation", e);
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
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	private boolean consensusParametersMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) throws ClassNotFoundException {
		if (response instanceof InitializationTransactionResponse)
			return true;

		// we check if there are events of type ConsensusUpdate triggered by the manifest, validators, gas station or versions
		try {
			if (isInitializedUncommitted() && response instanceof TransactionResponseWithEvents trwe) {
				Stream<StorageReference> events = trwe.getEvents();
				StorageReference manifest = node.getManifestUncommitted().get();
				StorageReference gasStation = getGasStationUncommitted().get();
				StorageReference versions = getVersionsUncommitted().get();
				StorageReference validators = getValidatorsUncommitted().get();

				return check(ClassNotFoundException.class, () ->
				events.filter(uncheck(event -> isConsensusUpdateEvent(event, classLoader)))
					.map(node.getStoreUtilities()::getCreatorUncommitted)
					.anyMatch(creator -> creator.equals(manifest) || creator.equals(validators) || creator.equals(gasStation) || creator.equals(versions))
				);
			}
		}
		catch (StoreException | NodeException e) {
			logger.log(Level.SEVERE, "cannot check the consensus parameters", e);
		}

		return false;
	}

	/**
	 * Determines if the node is initialized, that is, its manifest has been set,
	 * although possibly not yet committed.
	 * 
	 * @return true if and only if that condition holds
	 * @throws StoreException 
	 */
	private boolean isInitializedUncommitted() throws StoreException {
		return node.getManifestUncommitted().isPresent();
	}

	private boolean isConsensusUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws ClassNotFoundException {
		return classLoader.isConsensusUpdateEvent(node.getStoreUtilities().getClassNameUncommitted(event));
	}

	/**
	 * Determines if the given response might change the gas price.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the gas price
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	private boolean gasPriceMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) throws ClassNotFoundException {
		if (response instanceof InitializationTransactionResponse)
			return true;

		try {
			// we check if there are events of type GasPriceUpdate triggered by the gas station
			if (isInitializedUncommitted() && response instanceof TransactionResponseWithEvents trwe) {
				Stream<StorageReference> events = trwe.getEvents();
				StorageReference gasStation = getGasStationUncommitted().get();

				return check(ClassNotFoundException.class, () ->
					events.filter(uncheck(event -> isGasPriceUpdateEvent(event, classLoader)))
					.map(node.getStoreUtilities()::getCreatorUncommitted)
					.anyMatch(gasStation::equals)
				);
			}
		}
		catch (StoreException | NodeException e) {
			logger.log(Level.SEVERE, "cannot check the gas price", e);
		}

		return false;
	}

	/**
	 * Determines if the given response might change the current inflation.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used to build the response
	 * @return true if the response changes the current inflation
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	private boolean inflationMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) throws ClassNotFoundException {
		if (response instanceof InitializationTransactionResponse)
			return true;

		try {
			// we check if there are events of type InflationUpdate triggered by the validators object
			if (isInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
				Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
				StorageReference validators = getValidatorsUncommitted().get();

				return check(ClassNotFoundException.class, () ->
					events.filter(uncheck(event -> isInflationUpdateEvent(event, classLoader)))
					.map(node.getStoreUtilities()::getCreatorUncommitted)
					.anyMatch(validators::equals)
				);
			}
		}
		catch (StoreException | NodeException e) {
			logger.log(Level.SEVERE, "cannot check the inflation", e);
		}

		return false;
	}

	private boolean isGasPriceUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws ClassNotFoundException {
		return classLoader.isGasPriceUpdateEvent(node.getStoreUtilities().getClassNameUncommitted(event));
	}

	private boolean isInflationUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws ClassNotFoundException {
		return classLoader.isInflationUpdateEvent(node.getStoreUtilities().getClassNameUncommitted(event));
	}
}