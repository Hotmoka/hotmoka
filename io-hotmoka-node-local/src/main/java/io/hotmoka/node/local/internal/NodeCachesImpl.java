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
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.InitializationTransactionResponse;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.responses.TransactionResponseWithEvents;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.LongValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.SimpleValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.NodeCache;
import io.hotmoka.node.local.api.UnsupportedVerificationVersionException;

/**
 * An implementation of the caches of a local node.
 */
public class NodeCachesImpl implements NodeCache {
	protected final static Logger logger = Logger.getLogger(NodeCachesImpl.class.getName());

	private final NodeInternal node;

	/**
	 * The cache for the requests.
	 */
	private final LRUCache<TransactionReference, TransactionRequest<?>> requests;

	/**
	 * The cache for the committed responses.
	 */
	private final LRUCache<TransactionReference, TransactionResponse> responses;

	/**
	 * Cached recent requests that have had their signature checked.
	 * This avoids repeated signature checking in {@link AbstractLocalNode#checkTransaction(TransactionRequest)}
	 * and {@link AbstractLocalNode#deliverTransaction(TransactionRequest)}.
	 */
	private final LRUCache<SignedTransactionRequest<?>, Boolean> checkedSignatures;

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
	public NodeCachesImpl(NodeInternal node, ConsensusConfig<?,?> consensus) {
		this.node = node;
		this.requests = new LRUCache<>(100, node.getConfig().getRequestCacheSize());
		this.responses = new LRUCache<>(100, node.getConfig().getResponseCacheSize());
		this.checkedSignatures = new LRUCache<>(100, 1000);
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

	@Override
	public final void recomputeConsensus() {
		try {
			StorageReference gasStation = getGasStation().get();
			StorageReference validators = getValidators().get();
			StorageReference versions = getVersions().get();
			TransactionReference takamakaCode = node.getStoreUtilities().getTakamakaCodeUncommitted().get();
			StorageReference manifest = node.getStore().getManifestUncommitted().get();
	
			String genesisTime = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GENESIS_TIME, manifest))).getValue();

			String chainId = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))).getValue();
	
			StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest));

			String publicKeyOfGamete = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, gamete))).getValue();

			long maxErrorLength = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_ERROR_LENGTH, manifest))).getValue();

			long maxDependencies = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_DEPENDENCIES, manifest))).getValue();

			long maxCumulativeSizeOfDependencies = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))).getValue();

			boolean allowsSelfCharged = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_SELF_CHARGED, manifest))).getValue();
	
			boolean allowsFaucet = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_UNSIGNED_FAUCET, manifest))).getValue();

			boolean skipsVerification = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.SKIPS_VERIFICATION, manifest))).getValue();

			String signature = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_SIGNATURE, manifest))).getValue();

			BigInteger ticketForNewPoll = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_TICKET_FOR_NEW_POLL, validators))).getValue();

			BigInteger initialGasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_GAS_PRICE, gasStation))).getValue();

			BigInteger maxGasPerTransaction = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, gasStation))).getValue();
	
			boolean ignoresGasPrice = ((BooleanValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.IGNORES_GAS_PRICE, gasStation))).getValue();
	
			BigInteger targetGasAtReward = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_TARGET_GAS_AT_REWARD, gasStation))).getValue();
	
			long oblivion = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_OBLIVION, gasStation))).getValue();
	
			long initialInflation = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_INFLATION, validators))).getValue();

			long verificationVersion = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERIFICATION_VERSION, versions))).getValue();

			BigInteger initialSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_SUPPLY, validators))).getValue();

			BigInteger initialRedSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_RED_SUPPLY, validators))).getValue();

			BigInteger finalSupply = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_FINAL_SUPPLY, validators))).getValue();

			int buyerSurcharge = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getBuyerSurcharge", StorageTypes.INT), validators))).getValue();

			int slashingForMisbehaving = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getSlashingForMisbehaving", StorageTypes.INT), validators))).getValue();

			int slashingForNotBehaving = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getSlashingForNotBehaving", StorageTypes.INT), validators))).getValue();

			int percentStaked = ((IntValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.of(StorageTypes.VALIDATORS, "getPercentStaked", StorageTypes.INT), validators))).getValue();

			consensus = SimpleValidatorsConsensusConfigBuilders.defaults()
				.setGenesisTime(LocalDateTime.parse(genesisTime, DateTimeFormatter.ISO_DATE_TIME))
				.setChainId(chainId)
				.setMaxGasPerTransaction(maxGasPerTransaction)
				.ignoreGasPrice(ignoresGasPrice)
				.signRequestsWith(SignatureAlgorithms.of(signature))
				.setInitialGasPrice(initialGasPrice)
				.setTargetGasAtReward(targetGasAtReward)
				.setOblivion(oblivion)
				.setInitialInflation(initialInflation)
				.setMaxErrorLength(maxErrorLength)
				.setMaxDependencies(maxDependencies)
				.setMaxCumulativeSizeOfDependencies(maxCumulativeSizeOfDependencies)
				.allowSelfCharged(allowsSelfCharged)
				.allowUnsignedFaucet(allowsFaucet)
				.skipVerification(skipsVerification)
				.setVerificationVersion(verificationVersion)
				.setTicketForNewPoll(ticketForNewPoll)
				.setInitialSupply(initialSupply)
				.setFinalSupply(finalSupply)
				.setInitialRedSupply(initialRedSupply)
				.setPublicKeyOfGamete(publicKeyOfGamete)
				.setPercentStaked(percentStaked)
				.setBuyerSurcharge(buyerSurcharge)
				.setSlashingForMisbehaving(slashingForMisbehaving)
				.setSlashingForNotBehaving(slashingForNotBehaving)
				.build();
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "could not reconstruct the consensus parameters from the manifest", e);
			throw new RuntimeException("could not reconstruct the consensus parameters from the manifest", e);
		}
	}

	@Override
	public final Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		Objects.requireNonNull(reference);

		return requests.computeIfAbsentOptional(reference, _reference -> node.getStore().getRequest(_reference));
	}

	@Override
	public final Optional<TransactionResponse> getResponse(TransactionReference reference) {
		Objects.requireNonNull(reference);

		return responses.computeIfAbsentOptional(reference, _reference -> node.getStore().getResponse(_reference));
	}

	@Override
	public final Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		return getResponse(reference).or(() -> node.getStore().getResponseUncommitted(reference));
	}

	@Override
	public final EngineClassLoader getClassLoader(TransactionReference classpath) throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException {
		var classLoader = classLoaders.get(classpath);
		if (classLoader != null)
			return classLoader;

		var classLoader2 = new EngineClassLoaderImpl(null, Stream.of(classpath), node, true, consensus);
		return classLoaders.computeIfAbsent(classpath, _classpath -> classLoader2);
	}

	@Override
	public final boolean signatureIsValid(SignedTransactionRequest<?> request, SignatureAlgorithm signatureAlgorithm) throws Exception {
		return checkedSignatures.computeIfAbsent(request, _request -> verifiesSignature(signatureAlgorithm, request));
	}

	private boolean verifiesSignature(SignatureAlgorithm signature, SignedTransactionRequest<?> request) throws GeneralSecurityException, Base64ConversionException {
		return signature.getVerifier(getPublicKey(request.getCaller(), signature), SignedTransactionRequest<?>::toByteArrayWithoutSignature).verify(request, request.getSignature());
	}

	@Override
	public final ConsensusConfig<?,?> getConsensusParams() {
		return consensus;
	}

	@Override
	public final Optional<StorageReference> getGamete() {
		if (gamete.isEmpty())
			gamete = node.getStoreUtilities().getGameteUncommitted();

		return gamete;
	}

	@Override
	public final Optional<StorageReference> getValidators() {
		if (validators.isEmpty())
			validators = node.getStoreUtilities().getValidatorsUncommitted();

		return validators;
	}

	@Override
	public final Optional<StorageReference> getVersions() {
		if (versions.isEmpty())
			versions = node.getStoreUtilities().getVersionsUncommitted();

		return versions;
	}

	@Override
	public final Optional<StorageReference> getGasStation() {
		if (gasStation.isEmpty())
			gasStation = node.getStoreUtilities().getGasStationUncommitted();
	
		return gasStation;
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

	/**
	 * Yields the public key of the given externally owned account.
	 * 
	 * @param reference the account
	 * @param signatureAlgorithm the signing algorithm used for the request
	 * @return the public key
	 * @throws NoSuchAlgorithmException if the signing algorithm is unknown
	 * @throws NoSuchProviderException if the signing provider is unknown
	 * @throws InvalidKeySpecException if the key specification is invalid
	 * @throws Base64ConversionException if the public key of the account is not legally Base64 encoded
	 */
	private PublicKey getPublicKey(StorageReference reference, SignatureAlgorithm signatureAlgorithm) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, Base64ConversionException {
		// we go straight to the transaction that created the object
		String publicKeyEncodedBase64 = node.getStoreUtilities().getPublicKeyUncommitted(reference);
		byte[] publicKeyEncoded = Base64.fromBase64String(publicKeyEncodedBase64);
		return signatureAlgorithm.publicKeyFromEncoding(publicKeyEncoded);
	}

	private void recomputeGasPrice() {
		Optional<StorageReference> manifest = node.getStore().getManifestUncommitted();
		if (manifest.isPresent())
			try {
				gasPrice = ((BigIntegerValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest.get(), _100_000, node.getStoreUtilities().getTakamakaCodeUncommitted().get(),
							MethodSignatures.GET_GAS_PRICE, getGasStation().get()))).getValue();
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new RuntimeException("could not determine the gas price", e);
			}
	}

	private void recomputeInflation() {
		Optional<StorageReference> manifest = node.getStore().getManifestUncommitted();
		if (manifest.isPresent())
			try {
				inflation = ((LongValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest.get(), _100_000, node.getStoreUtilities().getTakamakaCodeUncommitted().get(),
							MethodSignatures.GET_CURRENT_INFLATION, getValidators().get()))).getValue();
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
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
		if (isInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
			Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
			StorageReference manifest = node.getStore().getManifestUncommitted().get();
			StorageReference gasStation = getGasStation().get();
			StorageReference versions = getVersions().get();
			StorageReference validators = getValidators().get();

			return check(ClassNotFoundException.class, () ->
				events.filter(uncheck(event -> isConsensusUpdateEvent(event, classLoader)))
					.map(node.getStoreUtilities()::getCreatorUncommitted)
					.anyMatch(creator -> creator.equals(manifest) || creator.equals(validators) || creator.equals(gasStation) || creator.equals(versions))
			);
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

		// we check if there are events of type GasPriceUpdate triggered by the gas station
		if (isInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
			Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
			StorageReference gasStation = getGasStation().get();

			return check(ClassNotFoundException.class, () ->
				events.filter(uncheck(event -> isGasPriceUpdateEvent(event, classLoader)))
					.map(node.getStoreUtilities()::getCreatorUncommitted)
					.anyMatch(gasStation::equals)
			);
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

		// we check if there are events of type InflationUpdate triggered by the validators object
		if (isInitializedUncommitted() && response instanceof TransactionResponseWithEvents) {
			Stream<StorageReference> events = ((TransactionResponseWithEvents) response).getEvents();
			StorageReference validators = getValidators().get();

			return check(ClassNotFoundException.class, () ->
				events.filter(uncheck(event -> isInflationUpdateEvent(event, classLoader)))
					.map(node.getStoreUtilities()::getCreatorUncommitted)
					.anyMatch(validators::equals)
			);
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