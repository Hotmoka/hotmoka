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

package io.hotmoka.node.local.internal.builders;

import static io.hotmoka.node.MethodSignatures.GET_CURRENT_INFLATION;
import static io.hotmoka.node.MethodSignatures.GET_GAS_PRICE;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.CheckSupplier;
import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
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
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.ResponseBuilder;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;

/**
 * An executor environment abstract both a store and a store transformation and allows
 * the execution of run transactions in both.
 */
public abstract class ExecutionEnvironment {
	private final static Logger LOGGER = Logger.getLogger(ExecutionEnvironment.class.getName());

	/**
	 * The executors to use to spawn new tasks.
	 */
	private final ExecutorService executors;

	/**
	 * Enough gas for a simple get method.
	 */
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Creates an execution environment whose transactions are executed with the given executors.
	 * 
	 * @param executors the executors to use to spawn new tasks
	 */
	protected ExecutionEnvironment(ExecutorService executors) {
		this.executors = executors;
	}

	public final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException, InterruptedException {
		return getOutcome(new InstanceViewMethodCallResponseBuilder(reference, request, this).getResponseCreation().getResponse());
	}

	public final Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException, InterruptedException {
		return getOutcome(new StaticViewMethodCallResponseBuilder(reference, request, this).getResponseCreation().getResponse());
	}

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method is called only when
	 * the transaction has been already committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 */
	public abstract TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 */
	public abstract TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the history of the given object, that is, the references to the transactions
	 * that can be used to reconstruct the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history
	 * @throws StoreException if the store is not able to perform the operation
	 */
	public abstract Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the manifest installed when the node is initialized.
	 * 
	 * @return the manifest
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	public abstract Optional<StorageReference> getManifest() throws StoreException;

	/**
	 * Yields the current consensus configuration of the node.
	 * 
	 * @return the current consensus configuration of the node
	 */
	public final ConsensusConfig<?,?> getConfig() {
		return getCache().getConfig();
	}

	protected final ConsensusConfig<?,?> extractConsensus() throws StoreException, InterruptedException {
		Optional<StorageReference> maybeManifest = getManifest();
		if (maybeManifest.isEmpty())
			throw new StoreException("Cannot extract the consensus if the manifest is not set yet");

		try {
			// enough gas for a simple get method
			BigInteger _100_000 = BigInteger.valueOf(100_000L);
	
			StorageReference manifest = maybeManifest.get();
			TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));

			// we cannot call getValidators(), getVersions() and getGasStation() since we are here exactly because the caches are not set yet
			StorageReference validators = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_VALIDATORS + " should not return void"))
					.asReference(value -> new StoreException(MethodSignatures.GET_VALIDATORS + " should return a reference, not a " + value.getClass().getName()));

			StorageReference gasStation = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAS_STATION, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_GAS_STATION + " should not return void"))
					.asReference(value -> new StoreException(MethodSignatures.GET_GAS_STATION + " should return a reference, not a " + value.getClass().getName()));

			StorageReference versions = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERSIONS, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_VERSIONS + " should not return void"))
					.asReference(value -> new StoreException(MethodSignatures.GET_VERSIONS + " should return a reference, not a " + value.getClass().getName()));
	
			String genesisTime = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_GENESIS_TIME, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_GENESIS_TIME + " should not return void"))
					.asString(value -> new StoreException(MethodSignatures.GET_GENESIS_TIME + " should return a string, not a " + value.getClass().getName()));
	
			String chainId = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_CHAIN_ID + " should not return void"))
					.asString(value -> new StoreException(MethodSignatures.GET_CHAIN_ID + " should return a string, not a " + value.getClass().getName()));
	
			StorageReference gamete = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_GAMETE + " should not return void"))
					.asReference(value -> new StoreException(MethodSignatures.GET_GAMETE + " should return a reference, not a " + value.getClass().getName()));
	
			String publicKeyOfGamete = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, gamete))
					.orElseThrow(() -> new StoreException(MethodSignatures.PUBLIC_KEY + " should not return void"))
					.asString(value -> new StoreException(MethodSignatures.PUBLIC_KEY + " should return a string, not a " + value.getClass().getName()));
	
			int maxErrorLength = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_ERROR_LENGTH, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_ERROR_LENGTH + " should not return void"))
					.asInt(value -> new StoreException(MethodSignatures.GET_MAX_ERROR_LENGTH + " should return an int, not a " + value.getClass().getName()));
	
			int maxDependencies = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_DEPENDENCIES, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_DEPENDENCIES + " should not return void"))
					.asInt(value -> new StoreException(MethodSignatures.GET_MAX_DEPENDENCIES + " should return an int, not a " + value.getClass().getName()));
	
			long maxCumulativeSizeOfDependencies = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES + " should not return void"))
					.asLong(value -> new StoreException(MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES + " should return a long, not a " + value.getClass().getName()));
	
			boolean allowsFaucet = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_UNSIGNED_FAUCET, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.ALLOWS_UNSIGNED_FAUCET + " should not return void"))
					.asBoolean(value -> new StoreException(MethodSignatures.ALLOWS_UNSIGNED_FAUCET + " should return a boolean, not a " + value.getClass().getName()));
	
			boolean skipsVerification = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.SKIPS_VERIFICATION, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.SKIPS_VERIFICATION + " should not return void"))
					.asBoolean(value -> new StoreException(MethodSignatures.SKIPS_VERIFICATION + " should return a boolean, not a " + value.getClass().getName()));
	
			String signature = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_SIGNATURE, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_SIGNATURE + " should not return void"))
					.asString(value -> new StoreException(MethodSignatures.GET_SIGNATURE + " should return a string, not a " + value.getClass().getName()));
	
			BigInteger ticketForNewPoll = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_TICKET_FOR_NEW_POLL, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_TICKET_FOR_NEW_POLL + " should not return void"))
					.asBigInteger(value -> new StoreException(MethodSignatures.GET_TICKET_FOR_NEW_POLL + " should return a BigInteger, not a " + value.getClass().getName()));
	
			BigInteger initialGasPrice = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_GAS_PRICE, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_GAS_PRICE + " should not return void"))
					.asBigInteger(value -> new StoreException(MethodSignatures.GET_INITIAL_GAS_PRICE + " should return a BigInteger, not a " + value.getClass().getName()));
	
			BigInteger maxGasPerTransaction = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_GAS_PER_TRANSACTION + " should not return void"))
					.asBigInteger(value -> new StoreException(MethodSignatures.GET_MAX_GAS_PER_TRANSACTION + " should return a BigInteger, not a " + value.getClass().getName()));
	
			boolean ignoresGasPrice = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.IGNORES_GAS_PRICE, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.IGNORES_GAS_PRICE + " should not return void"))
					.asBoolean(value -> new StoreException(MethodSignatures.IGNORES_GAS_PRICE + " should return a boolean, not a " + value.getClass().getName()));
	
			BigInteger targetGasAtReward = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_TARGET_GAS_AT_REWARD, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_TARGET_GAS_AT_REWARD + " should not return void"))
					.asBigInteger(value -> new StoreException(MethodSignatures.GET_TARGET_GAS_AT_REWARD + " should return a BigInteger, not a " + value.getClass().getName()));
	
			long oblivion = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_OBLIVION, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_OBLIVION + " should not return void"))
					.asLong(value -> new StoreException(MethodSignatures.GET_OBLIVION + " should return a long, not a " + value.getClass().getName()));
	
			long initialInflation = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_INFLATION, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_INFLATION + " should not return void"))
					.asLong(value -> new StoreException(MethodSignatures.GET_INITIAL_INFLATION + " should return a long, not a " + value.getClass().getName()));
	
			long verificationVersion = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERIFICATION_VERSION, versions))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_VERIFICATION_VERSION + " should not return void"))
					.asLong(value -> new StoreException(MethodSignatures.GET_VERIFICATION_VERSION + " should return a long, not a " + value.getClass().getName()));
	
			BigInteger initialSupply = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_SUPPLY, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_SUPPLY + " should not return void"))
					.asBigInteger(value -> new StoreException(MethodSignatures.GET_INITIAL_SUPPLY + " should return a BigInteger, not a " + value.getClass().getName()));
	
			BigInteger initialRedSupply = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_RED_SUPPLY, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_RED_SUPPLY + " should not return void"))
					.asBigInteger(value -> new StoreException(MethodSignatures.GET_INITIAL_RED_SUPPLY + " should return a BigInteger, not a " + value.getClass().getName()));
	
			BigInteger finalSupply = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_FINAL_SUPPLY, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_FINAL_SUPPLY + " should not return void"))
					.asBigInteger(value -> new StoreException(MethodSignatures.GET_FINAL_SUPPLY + " should return a BigInteger, not a " + value.getClass().getName()));
	
			int buyerSurcharge = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE + " should not return void"))
					.asInt(value -> new StoreException(MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE + " should return an int, not a " + value.getClass().getName()));
	
			int slashingForMisbehaving = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_SLASHING_FOR_MISBEHAVING, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.VALIDATORS_GET_SLASHING_FOR_MISBEHAVING + " should not return void"))
					.asInt(value -> new StoreException(MethodSignatures.VALIDATORS_GET_SLASHING_FOR_MISBEHAVING + " should return an int, not a " + value.getClass().getName()));
	
			int slashingForNotBehaving = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING + " should not return void"))
					.asInt(value -> new StoreException(MethodSignatures.VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING + " should return an int, not a " + value.getClass().getName()));
	
			int percentStaked = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_PERCENT_STAKED, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.VALIDATORS_GET_PERCENT_STAKED + " should not return void"))
					.asInt(value -> new StoreException(MethodSignatures.VALIDATORS_GET_PERCENT_STAKED + " should return an int, not a " + value.getClass().getName()));
	
			var signatureAlgorithm = SignatureAlgorithms.of(signature);
	
			return ValidatorsConsensusConfigBuilders.defaults()
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
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | Base64ConversionException e) {
			throw new StoreException(e);
		}
	}

	protected final StorageReference extractValidators() throws StoreException {
		Optional<StorageReference> manifest = getManifest();
		if (manifest.isPresent()) {
			try {
				return getReferenceField(manifest.get(), FieldSignatures.MANIFEST_VALIDATORS_FIELD);
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the validators set", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			throw new StoreException("The node has been initialized but its manifest cannot be found");
	}

	protected final StorageReference extractGasStation() throws StoreException {
		Optional<StorageReference> manifest = getManifest();
		if (manifest.isPresent()) {
			try {
				return getReferenceField(manifest.get(), FieldSignatures.MANIFEST_GAS_STATION_FIELD);
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the gas station", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			throw new StoreException("The node has been initialized but its manifest cannot be found");
	}

	protected final StorageReference extractVersions() throws StoreException {
		Optional<StorageReference> manifest = getManifest();
		if (manifest.isPresent()) {
			try {
				return getReferenceField(manifest.get(), FieldSignatures.MANIFEST_VERSIONS_FIELD);
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the versions manager", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}
		}
		else
			throw new StoreException("The node has been initialized but its manifest cannot be found");
	}

	protected final BigInteger extractGasPrice() throws StoreException, InterruptedException {
		Optional<StorageReference> manifest = getManifest();
		if (manifest.isPresent()) {
			TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
			StorageReference gasStation;

			try {
				gasStation = getReferenceField(manifest.get(), FieldSignatures.MANIFEST_GAS_STATION_FIELD);
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the gas station", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}

			try {
				return runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest.get(), _100_000, takamakaCode, GET_GAS_PRICE, gasStation))
					.orElseThrow(() -> new StoreException(GET_GAS_PRICE + " should not return void"))
					.asReturnedBigInteger(GET_GAS_PRICE, StoreException::new);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new StoreException(e);
			}
		}
		else
			throw new StoreException("The node has been initialized but its manifest cannot be found");
	}

	protected final long extractInflation() throws StoreException, InterruptedException {
		Optional<StorageReference> manifest = getManifest();
		if (manifest.isPresent()) {
			TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
			StorageReference validators;

			try {
				validators = getReferenceField(manifest.get(), FieldSignatures.MANIFEST_VALIDATORS_FIELD);
			}
			catch (FieldNotFoundException e) {
				throw new StoreException("The manifest does not contain the reference to the validators set", e);
			}
			catch (UnknownReferenceException e) {
				throw new StoreException("The manifest is set but cannot be found in store", e);
			}

			try {
				return runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest.get(), _100_000, takamakaCode, GET_CURRENT_INFLATION, validators))
					.orElseThrow(() -> new StoreException(GET_CURRENT_INFLATION + " should not return void"))
					.asLong(value -> new StoreException(GET_CURRENT_INFLATION + " should return a long, not a " + value.getClass().getName()));
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new StoreException(e);
			}
		}
		else
			throw new StoreException("The node has been initialized but its manifest cannot be found");
	}

	/**
	 * Yields a class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws StoreException if the store is not able to complete the operation correctly
	 * @throws TransactionRejectedException 
	 */
	protected final EngineClassLoader getClassLoader(TransactionReference classpath, ConsensusConfig<?,?> consensus) throws StoreException, TransactionRejectedException {
		return CheckSupplier.check(StoreException.class, TransactionRejectedException.class, () -> getClassLoader(classpath, UncheckFunction.uncheck(_classpath -> mkClassLoader(_classpath, consensus))));
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

	protected final String getClassName(StorageReference reference) throws UnknownReferenceException, StoreException {
		return getClassTag(reference).getClazz().getName();
	}

	protected final StorageReference getReferenceField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof StorageReference reference)
			return reference;
		else
			throw new FieldNotFoundException(field);
	}

	protected final UpdateOfField getLastUpdateToField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		Stream<TransactionReference> history = getHistory(object);

		try {
			return CheckSupplier.check(StoreException.class, UnknownReferenceException.class, () -> history.map(UncheckFunction.uncheck(transaction -> getLastUpdate(object, field, transaction)))
					.flatMap(Optional::stream)
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

	protected final BigInteger getBigIntegerField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof BigIntegerValue biv)
			return biv.getValue();
		else
			throw new FieldNotFoundException(field);
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

	protected final BigInteger getTotalBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBalance(contract).add(getRedBalance(contract));
	}

	protected final BigInteger getNonce(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(account, FieldSignatures.EOA_NONCE_FIELD);
	}

	protected final Stream<UpdateOfField> getEagerFields(StorageReference object) throws UnknownReferenceException, StoreException {
		var fieldsAlreadySeen = new HashSet<FieldSignature>();

		return getHistory(object)
				.flatMap(CheckSupplier.check(StoreException.class, () -> UncheckFunction.uncheck(this::getUpdates)))
				.filter(update -> update.isEager() && update instanceof UpdateOfField uof && update.getObject().equals(object) && fieldsAlreadySeen.add(uof.getField()))
				.map(update -> (UpdateOfField) update);
	}

	protected final Stream<Update> getUpdates(TransactionReference referenceInHistory) throws StoreException {
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

	protected final boolean signatureIsValid(SignedTransactionRequest<?> request, SignatureAlgorithm signatureAlgorithm) throws StoreException, UnknownReferenceException, FieldNotFoundException {
		var reference = TransactionReferences.of(getHasher().hash(request));
		return CheckSupplier.check(StoreException.class, UnknownReferenceException.class, FieldNotFoundException.class, () ->
			signatureIsValid(reference, UncheckFunction.uncheck(_reference -> verifySignature(signatureAlgorithm, request))));
	}

	protected final String getPublicKey(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getStringField(account, FieldSignatures.EOA_PUBLIC_KEY_FIELD);
	}

	protected final <X> Future<X> submit(Callable<X> task) {
		return executors.submit(task);
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
	protected final ResponseBuilder<?,?> responseBuilderFor(TransactionReference reference, TransactionRequest<?> request) throws TransactionRejectedException, StoreException {
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
			throw new StoreException("Unexpected transaction request of class " + request.getClass().getName());
	}

	protected final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException, InterruptedException {
		return runInstanceMethodCallTransaction(request, TransactionReferences.of(getHasher().hash(request)));
	}

	protected final ExecutorService getExecutors() {
		return executors;
	}

	/**
	 * Yields the time to use as current time for the requests executed inside this environment.
	 * 
	 * @return the time, in milliseconds from the UNIX epoch time
	 */
	public abstract long getNow();

	protected abstract StoreCache getCache();

	protected final Optional<StorageReference> getValidators() {
		return getCache().getValidators();
	}

	protected final Optional<StorageReference> getGasStation() {
		return getCache().getGasStation();
	};

	protected final Optional<StorageReference> getVersions() {
		return getCache().getVersions();
	}

	/**
	 * Yields the current gas price at the end of this transaction.
	 * This might be missing if the node is not initialized yet.
	 * 
	 * @return the current gas price at the end of this transaction
	 */
	protected final Optional<BigInteger> getGasPrice() {
		return getCache().getGasPrice();
	}

	protected final OptionalLong getInflation() {
		return getCache().getInflation();
	}

	protected final EngineClassLoader getClassLoader(TransactionReference classpath, Function<TransactionReference, EngineClassLoader> ifMissing) {
		return getCache().getClassLoader(classpath, ifMissing);
	}

	protected final boolean signatureIsValid(TransactionReference classpath, Function<TransactionReference, Boolean> ifMissing) {
		return getCache().getValidSignatureOutcome(classpath, ifMissing);
	}

	protected abstract Hasher<TransactionRequest<?>> getHasher();

	private EngineClassLoader mkClassLoader(TransactionReference classpath, ConsensusConfig<?,?> consensus) throws StoreException, TransactionRejectedException {
		return new EngineClassLoaderImpl(null, Stream.of(classpath), this, consensus);
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

	private BigInteger getBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(contract, FieldSignatures.BALANCE_FIELD);
	}

	private BigInteger getRedBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(contract, FieldSignatures.RED_BALANCE_FIELD);
	}

	private String getStringField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		StorageValue value = getLastUpdateToField(object, field).getValue();
		if (value instanceof StringValue sv)
			return sv.getValue();
		else
			throw new FieldNotFoundException(field);
	}
}