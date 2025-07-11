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
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
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
import io.hotmoka.node.api.ClassLoaderCreationException;
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
import io.hotmoka.node.api.responses.NonVoidMethodCallTransactionSuccessfulResponse;
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
import io.hotmoka.node.local.api.UncheckedStoreException;

/**
 * An executor environment abstract both a store and a store transformation and allows
 * the execution of run transactions in both.
 */
public abstract class ExecutionEnvironment {
	private final static Logger LOGGER = Logger.getLogger(ExecutionEnvironment.class.getName());

	/**
	 * Enough gas for a simple get method.
	 */
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Creates an execution environment.
	 */
	protected ExecutionEnvironment() {}

	/**
	 * Runs an instance {@code @@View} method of an object already in this environment.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception; this is empty
	 *         if and only if the method is declared to return {@code void}
	 * @throws TransactionRejectedException if the transaction could not be executed
	 * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code,
	 *                                that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed but led to an exception outside the user code,
	 *                              or that is not allowed to be thrown by the method
	 * @throws StoreException if the store is misbehaving
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException, InterruptedException {
		return getOutcome(new InstanceViewMethodCallResponseBuilder(reference, request, this).getResponseCreation().getResponse());
	}

	/**
	 * Runs a static {@code @@View} method of a class in this environment.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception; this is empty
	 *         if and only if the method is declared to return {@code void}
	 * @throws TransactionRejectedException if the transaction could not be executed
	 * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code,
	 *                                that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed but led to an exception outside the user code,
	 *                              or that is not allowed to be thrown by the method
	 * @throws StoreException if the store is misbehaving
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public final Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException, InterruptedException {
		return getOutcome(new StaticViewMethodCallResponseBuilder(reference, request, this).getResponseCreation().getResponse());
	}

	/**
	 * Yields the current consensus configuration of the node.
	 * 
	 * @return the current consensus configuration of the node
	 */
	public final ConsensusConfig<?,?> getConfig() {
		return getCache().getConfig();
	}

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 * @throws UnknownReferenceException if the request of {@code reference} cannot be found in this node
	 */
	public abstract TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException;

	/**
	 * Yields the response of the transaction having the given reference.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 * @throws UnknownReferenceException if the response of {@code reference} cannot be found in this environment
	 */
	public abstract TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException;

	/**
	 * Yields the history of the given object, that is, the references to the transactions
	 * that can be used to reconstruct the current values of its fields.
	 * 
	 * @param object the reference of the object
	 * @return the history
	 * @throws UnknownReferenceException if {@code object} cannot be found in this environment
	 */
	public abstract Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException;

	/**
	 * Yields the manifest installed when the node is initialized.
	 * 
	 * @return the manifest
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	public abstract Optional<StorageReference> getManifest();

	/**
	 * Yields the time to use as current time for the requests executed inside this environment.
	 * 
	 * @return the time, in milliseconds from the UNIX epoch time
	 */
	public abstract long getNow();

	/**
	 * Reconstructs the consensus information from the information inside this environment.
	 * 
	 * @param manifest the reference to the manifest; this is assumed to actually refer to a manifest
	 * @return the reconstructed consensus information
	 * @throws StoreException if the store is not able to complete the operation correctly
	 * @throws InterruptedException if the current thread is interrupted while performing this operation
	 */
	protected final ConsensusConfig<?,?> extractConsensus(StorageReference manifest) throws StoreException, InterruptedException {
		try {
			TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));

			// we cannot call getValidators(), getVersions() and getGasStation() since we are here exactly because the caches are not set yet
			StorageReference validators = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VALIDATORS, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_VALIDATORS + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_VALIDATORS, StoreException::new);

			StorageReference gasStation = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAS_STATION, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_GAS_STATION + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_GAS_STATION, StoreException::new);

			StorageReference versions = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERSIONS, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_VERSIONS + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_VERSIONS, StoreException::new);
	
			String genesisTime = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_GENESIS_TIME, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_GENESIS_TIME + " should not return void"))
					.asReturnedString(MethodSignatures.GET_GENESIS_TIME, StoreException::new);
	
			String chainId = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_CHAIN_ID + " should not return void"))
					.asReturnedString(MethodSignatures.GET_CHAIN_ID, StoreException::new);
	
			StorageReference gamete = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_GAMETE + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_GAMETE, StoreException::new);
	
			String publicKeyOfGamete = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.PUBLIC_KEY, gamete))
					.orElseThrow(() -> new StoreException(MethodSignatures.PUBLIC_KEY + " should not return void"))
					.asReturnedString(MethodSignatures.PUBLIC_KEY, StoreException::new);
	
			int maxErrorLength = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_ERROR_LENGTH, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_ERROR_LENGTH + " should not return void"))
					.asReturnedInt(MethodSignatures.GET_MAX_ERROR_LENGTH, StoreException::new);
	
			int maxDependencies = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_DEPENDENCIES, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_DEPENDENCIES + " should not return void"))
					.asReturnedInt(MethodSignatures.GET_MAX_DEPENDENCIES, StoreException::new);
	
			long maxCumulativeSizeOfDependencies = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES + " should not return void"))
					.asReturnedLong(MethodSignatures.GET_MAX_CUMULATIVE_SIZE_OF_DEPENDENCIES, StoreException::new);
	
			boolean allowsFaucet = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.ALLOWS_UNSIGNED_FAUCET, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.ALLOWS_UNSIGNED_FAUCET + " should not return void"))
					.asReturnedBoolean(MethodSignatures.ALLOWS_UNSIGNED_FAUCET, StoreException::new);
	
			boolean skipsVerification = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.SKIPS_VERIFICATION, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.SKIPS_VERIFICATION + " should not return void"))
					.asReturnedBoolean(MethodSignatures.SKIPS_VERIFICATION, StoreException::new);
	
			String signature = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_SIGNATURE, manifest))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_SIGNATURE + " should not return void"))
					.asReturnedString(MethodSignatures.GET_SIGNATURE, StoreException::new);
	
			BigInteger ticketForNewPoll = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_TICKET_FOR_NEW_POLL, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_TICKET_FOR_NEW_POLL + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.GET_TICKET_FOR_NEW_POLL, StoreException::new);
	
			BigInteger initialGasPrice = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_GAS_PRICE, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_GAS_PRICE + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.GET_INITIAL_GAS_PRICE, StoreException::new);
	
			BigInteger maxGasPerTransaction = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_MAX_GAS_PER_TRANSACTION + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.GET_MAX_GAS_PER_TRANSACTION, StoreException::new);
	
			boolean ignoresGasPrice = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.IGNORES_GAS_PRICE, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.IGNORES_GAS_PRICE + " should not return void"))
					.asReturnedBoolean(MethodSignatures.IGNORES_GAS_PRICE, StoreException::new);
	
			BigInteger targetGasAtReward = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_TARGET_GAS_AT_REWARD, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_TARGET_GAS_AT_REWARD + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.GET_TARGET_GAS_AT_REWARD, StoreException::new);
	
			long oblivion = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_OBLIVION, gasStation))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_OBLIVION + " should not return void"))
					.asReturnedLong(MethodSignatures.GET_OBLIVION, StoreException::new);
	
			long verificationVersion = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_VERIFICATION_VERSION, versions))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_VERIFICATION_VERSION + " should not return void"))
					.asReturnedLong(MethodSignatures.GET_VERIFICATION_VERSION, StoreException::new);

			long initialInflation = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_INFLATION, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_INFLATION + " should not return void"))
					.asReturnedLong(MethodSignatures.GET_INITIAL_INFLATION, StoreException::new);
	
			BigInteger initialSupply = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_INITIAL_SUPPLY, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_INITIAL_SUPPLY + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.GET_INITIAL_SUPPLY, StoreException::new);
	
			BigInteger finalSupply = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_FINAL_SUPPLY, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_FINAL_SUPPLY + " should not return void"))
					.asReturnedBigInteger(MethodSignatures.GET_FINAL_SUPPLY, StoreException::new);
	
			int buyerSurcharge = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE + " should not return void"))
					.asReturnedInt(MethodSignatures.VALIDATORS_GET_BUYER_SURCHARGE, StoreException::new);
	
			int slashingForMisbehaving = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_SLASHING_FOR_MISBEHAVING, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.VALIDATORS_GET_SLASHING_FOR_MISBEHAVING + " should not return void"))
					.asReturnedInt(MethodSignatures.VALIDATORS_GET_SLASHING_FOR_MISBEHAVING, StoreException::new);
	
			int slashingForNotBehaving = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING + " should not return void"))
					.asReturnedInt(MethodSignatures.VALIDATORS_GET_SLASHING_FOR_NOT_BEHAVING, StoreException::new);
	
			int percentStaked = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.VALIDATORS_GET_PERCENT_STAKED, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.VALIDATORS_GET_PERCENT_STAKED + " should not return void"))
					.asReturnedInt(MethodSignatures.VALIDATORS_GET_PERCENT_STAKED, StoreException::new);
	
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
					.setPublicKeyOfGamete(signatureAlgorithm.publicKeyFromEncoding(Base64.fromBase64String(publicKeyOfGamete)))
					.setPercentStaked(percentStaked)
					.setBuyerSurcharge(buyerSurcharge)
					.setSlashingForMisbehaving(slashingForMisbehaving)
					.setSlashingForNotBehaving(slashingForNotBehaving)
					.build();
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | Base64ConversionException | DateTimeParseException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Extracts the validators object from the given manifest.
	 * 
	 * @param manifest the reference to the manifest; this is assumed to actually refer to a manifest
	 * @return the validators object reference
	 * @throws StoreException if the store is misbehaving
	 */
	protected final StorageReference extractValidators(StorageReference manifest) throws StoreException {
		try {
			return getReferenceField(manifest, FieldSignatures.MANIFEST_VALIDATORS_FIELD);
		}
		catch (FieldNotFoundException e) {
			throw new StoreException("The manifest does not contain the reference to the validators set", e);
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("The manifest is set but cannot be found in store", e);
		}
	}

	/**
	 * Extracts the gas station object from the given manifest.
	 * 
	 * @param manifest the reference to the manifest; this is assumed to actually refer to a manifest
	 * @return the gas station object reference
	 * @throws StoreException if the store is misbehaving
	 */
	protected final StorageReference extractGasStation(StorageReference manifest) throws StoreException {
		try {
			return getReferenceField(manifest, FieldSignatures.MANIFEST_GAS_STATION_FIELD);
		}
		catch (FieldNotFoundException e) {
			throw new StoreException("The manifest does not contain the reference to the gas station", e);
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("The manifest is set but cannot be found in store", e);
		}
	}

	/**
	 * Extracts the versions object from the given manifest.
	 * 
	 * @param manifest the reference to the manifest; this is assumed to actually refer to a manifest
	 * @return the versions object reference
	 * @throws StoreException if the store is misbehaving
	 */
	protected final StorageReference extractVersions(StorageReference manifest) throws StoreException {
		try {
			return getReferenceField(manifest, FieldSignatures.MANIFEST_VERSIONS_FIELD);
		}
		catch (FieldNotFoundException e) {
			throw new StoreException("The manifest does not contain the reference to the versions manager", e);
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("The manifest is set but cannot be found in store", e);
		}
	}

	/**
	 * Extracts the gas price from the given manifest.
	 * 
	 * @param manifest the reference to the manifest; this is assumed to actually refer to a manifest
	 * @return the gas price, as reported in the gas station of {@code manifest}
	 * @throws StoreException if the store is misbehaving
	 */
	protected final BigInteger extractGasPrice(StorageReference manifest) throws StoreException, InterruptedException {
		StorageReference gasStation = extractGasStation(manifest);
		TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));

		try {
			return runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, GET_GAS_PRICE, gasStation))
					.orElseThrow(() -> new StoreException(GET_GAS_PRICE + " should not return void"))
					.asReturnedBigInteger(GET_GAS_PRICE, StoreException::new);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			// the call to getGasPrice() should raise no exception
			throw new StoreException(e);
		}
	}

	/**
	 * Extracts the inflation from the given manifest.
	 * 
	 * @param manifest the reference to the manifest; this is assumed to actually refer to a manifest
	 * @return the inflation, as reported in the validators of {@code manifest}
	 * @throws StoreException if the store is misbehaving
	 */
	protected final long extractInflation(StorageReference manifest) throws StoreException, InterruptedException {
		StorageReference validators = extractValidators(manifest);
		TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));

		try {
			return runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(manifest, _100_000, takamakaCode, GET_CURRENT_INFLATION, validators))
					.orElseThrow(() -> new StoreException(GET_CURRENT_INFLATION + " should not return void"))
					.asReturnedLong(GET_CURRENT_INFLATION, StoreException::new);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Yields a class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws StoreException if the store is not able to complete the operation correctly
	 * @throws ClassLoaderCreationException if the class loader cannot be created
	 */
	protected final EngineClassLoader getClassLoader(TransactionReference classpath, ConsensusConfig<?,?> consensus) throws StoreException, ClassLoaderCreationException {
		return getCache().getClassLoader(classpath, _classpath -> new EngineClassLoaderImpl(null, Stream.of(_classpath), this, consensus));
	}

	/**
	 * Yields the class tag of the object at the given reference in store.
	 * 
	 * @param reference the reference to the object in store
	 * @return the class tag of the object at {@code reference}
	 * @throws UnknownReferenceException if {@code reference} does not refer to an object in store
	 */
	protected final ClassTag getClassTag(StorageReference reference) throws UnknownReferenceException {
		// we go straight to the transaction that created the object
		if (getResponse(reference.getTransaction()) instanceof TransactionResponseWithUpdates trwu) {
			return trwu.getUpdates().filter(update -> update instanceof ClassTag && update.getObject().equals(reference))
					.map(update -> (ClassTag) update)
					.findFirst() // TODO: hotspot
					.orElseThrow(() -> new UnknownReferenceException("Object " + reference + " does not exist"));
		}
		else
			throw new UnknownReferenceException("Transaction reference " + reference + " does not contain updates");
	}

	/**
	 * Yields the name of the class of the object in store at the given reference.
	 * 
	 * @param reference the reference of the object
	 * @return the class name of the object at {@code reference}
	 * @throws UnknownReferenceException if {@code reference} is not bound to any object in store
	 * @throws StoreException if the store is misbehaving
	 */
	protected final String getClassName(StorageReference reference) throws UnknownReferenceException, StoreException {
		return getClassTag(reference).getClazz().getName();
	}

	/**
	 * Yields the value of the given field of the given object in store, of type reference.
	 * 
	 * @param object the reference to the object in store
	 * @param field the field
	 * @return the value of {@code field} of {@code object}
	 * @throws UnknownReferenceException if {@code object} cannot be found in store
	 * @throws FieldNotFoundException if the field cannot be found or its type is not reference
	 * @throws StoreException if the store is misbehaving
	 */
	protected final StorageReference getReferenceField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		if (getLastUpdateToField(object, field).getValue() instanceof StorageReference reference)
			return reference;
		else
			throw new FieldNotFoundException(field);
	}

	/**
	 * Yields the value of the given field of the given object in store, of type {@code BigInteger}.
	 * 
	 * @param object the reference to the object in store
	 * @param field the field
	 * @return the value of {@code field} of {@code object}
	 * @throws UnknownReferenceException if {@code object} cannot be found in store
	 * @throws FieldNotFoundException if the field cannot be found or its type is not {@code BigInteger}
	 * @throws StoreException if the store is misbehaving
	 */
	protected final BigInteger getBigIntegerField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		if (getLastUpdateToField(object, field).getValue() instanceof BigIntegerValue biv)
			return biv.getValue();
		else
			throw new FieldNotFoundException(field);
	}

	/**
	 * Yields the last update to the given field of the given object in store.
	 * If the field is {@code final}, it is more optimized to call
	 * {@link #getLastUpdateToFinalField(StorageReference, FieldSignature)}.
	 * 
	 * @param object the reference to the object in store
	 * @param field the field
	 * @return the last update of {@code field} of {@code object}
	 * @throws UnknownReferenceException if {@code object} cannot be found in store
	 * @throws FieldNotFoundException if {@code object} has not {@code field}
	 * @throws StoreException if the store is misbehaving
	 */
	protected final UpdateOfField getLastUpdateToField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		Stream<TransactionReference> history = getHistory(object);

		try {
			// YoutKit suggests that this is a hotspot if streams are used instead of iterative programming
			for (var transaction: history.toArray(TransactionReference[]::new)) {
				var lastUpdate = getUpdateFromTransactionInHistory(object, field, transaction);
				if (lastUpdate.isPresent())
					return lastUpdate.get();
			}

			throw new FieldNotFoundException(field);
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Object " + object + " has a history containing a reference not in store");
		}
	}

	/**
	 * Yields the last update to the given {@code final} field of the given object in store.
	 * If the field is not {@code final}, then {@link #getLastUpdateToField(StorageReference, FieldSignature)}
	 * should be called instead.
	 * 
	 * @param object the reference to the object in store
	 * @param field the field; this is assumed to be {@code final}
	 * @return the last update of {@code field} of {@code object}
	 * @throws UnknownReferenceException if {@code object} cannot be found in store
	 * @throws FieldNotFoundException if {@code object} has not {@code field}
	 */
	protected final UpdateOfField getLastUpdateToFinalField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException {
		// it accesses directly the transaction that created the object
		return getUpdateFromTransactionInHistory(object, field, object.getTransaction()).orElseThrow(() -> new FieldNotFoundException(field));
	}

	/**
	 * Yields the gamete of the node.
	 * 
	 * @return the gamete, if it is already set
	 * @throws StoreException if the store is misbehaving
	 */
	protected final Optional<StorageReference> getGamete() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isEmpty())
			return Optional.empty();

		try {
			return Optional.of(getReferenceField(maybeManifest.get(), FieldSignatures.MANIFEST_GAMETE_FIELD));
		}
		catch (FieldNotFoundException e) {
			throw new StoreException("The manifest does not contain the reference to the gamete", e);
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("The manifest is set but it cannot be found in store", e);
		}
	}

	/**
	 * Yields the Takamaka code of the node, that is, the reference to the jar
	 * that installed its manifest.
	 * 
	 * @return the Takamaka code, if it is already set
	 * @throws StoreException if the store is misbehaving
	 */
	protected final Optional<TransactionReference> getTakamakaCode() throws StoreException {
		var maybeManifest = getManifest();
		if (maybeManifest.isEmpty())
			return Optional.empty();

		try {
			return Optional.of(getClassTag(maybeManifest.get()).getJar());
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("The manifest is set but its class tag cannot be found", e);
		}
	}

	/**
	 * Yields the nonce of the given account.
	 * 
	 * @param account the account
	 * @return the nonce of {@code account}
	 * @throws UnknownReferenceException if {@code account} is not found in store
	 * @throws FieldNotFoundException if {@code account} has no field holding its nonce; this means that it is not really an account
	 * @throws StoreException if the store is misbehaving
	 */
	protected final BigInteger getNonce(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(account, FieldSignatures.EOA_NONCE_FIELD);
	}

	/**
	 * Yields the public key of the given account.
	 * 
	 * @param account the account
	 * @return the public key of {@code account}
	 * @throws UnknownReferenceException if {@code account} is not found in store
	 * @throws FieldNotFoundException if {@code account} has no field holding its public key; this means that it is not really an account
	 * @throws StoreException if the store is misbehaving
	 */
	protected final String getPublicKey(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getStringField(account, FieldSignatures.EOA_PUBLIC_KEY_FIELD);
	}

	/**
	 * Yields the balance of the given contract.
	 * 
	 * @param contract the contract
	 * @return the balance of {@code contract}
	 * @throws UnknownReferenceException if {@code contract} is not found in store
	 * @throws FieldNotFoundException if {@code contract} has no field holding its balance; this means that it is not really a contract
	 * @throws StoreException if the store is misbehaving
	 */
	protected final BigInteger getBalance(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getBigIntegerField(contract, FieldSignatures.BALANCE_FIELD);
	}

	/**
	 * Yields the set of updates describing the value of the eager fields of the given object.
	 * 
	 * @param object the object
	 * @return the set of updates
	 * @throws UnknownReferenceException if {@code object} cannot be found in store
	 * @throws StoreException if the store is misbehaving
	 */
	protected final Stream<UpdateOfField> getEagerFields(StorageReference object) throws UnknownReferenceException, StoreException {
		var fieldsAlreadySeen = new HashSet<FieldSignature>();

		return getHistory(object)
			.flatMap(CheckSupplier.check(StoreException.class, () -> UncheckFunction.uncheck(StoreException.class, this::getUpdates)))
			.filter(update -> update.isEager() && update instanceof UpdateOfField uof && update.getObject().equals(object) && fieldsAlreadySeen.add(uof.getField()))
			.map(update -> (UpdateOfField) update);
	}

	/**
	 * Yields the updates resulting from the execution of the transaction with the given reference.
	 * It assumes that the transaction is part of the history of some object, hence it should exist
	 * in store and should have generated updates.
	 * 
	 * @param referenceInHistory the reference to the transaction; this is assumed to be part of the
	 *                           history of some object
	 * @return the updates resulting from the execution of {@code referenceInHistory}
	 * @throws StoreException if the store is misbehaving
	 */
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

	/**
	 * Verifies the signature of a request is valid, by using a cache to avoid repeated checks, if possible.
	 * 
	 * @param request the request
	 * @param algorithm the signature algorithm to use for checking the validity of the signature
	 * @return true if and only if the signature of {@code request} could be successfully validated
	 * @throws StoreException if the store is not able to complete the operation correctly
	 * @throws UnknownReferenceException if the caller of the request cannot be found in store
	 * @throws FieldNotFoundException if the caller of the request has no field for its public key; hence it is not really an account
	 */
	protected final boolean signatureIsValid(SignedTransactionRequest<?> request, SignatureAlgorithm algorithm) throws StoreException, UnknownReferenceException, FieldNotFoundException {
		return getCache().signatureIsValid(TransactionReferences.of(getHasher().hash(request)), _reference -> verifySignature(request, algorithm));
	}

	/**
	 * Runs the given task with the executors of the node.
	 * 
	 * @param <X> the type of the result of the task
	 * @param task the task
	 * @return a future of the task
	 */
	protected abstract <X> Future<X> submit(Callable<X> task);

	/**
	 * Yields the builder of a response for a request of a transaction.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @return the builder
	 * @throws StoreException if the store is misbehaving
	 */
	protected final ResponseBuilder<?,?> responseBuilderFor(TransactionReference reference, TransactionRequest<?> request) throws StoreException {
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

	/**
	 * Runs the given instance method call transaction in this environment.
	 * 
	 * @param request the request of the transaction to run
	 * @return the result of the method call, if the method is not {@code void}
	 */
	protected final Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException, InterruptedException {
		return runInstanceMethodCallTransaction(request, TransactionReferences.of(getHasher().hash(request)));
	}

	/**
	 * Yields the cache used by the store of the environment.
	 * 
	 * @return the cache
	 */
	protected abstract StoreCache getCache();

	/**
	 * Yields the validators in store.
	 * 
	 * @return the validators; this is missing if the node is not initialized yet
	 */
	protected final Optional<StorageReference> getValidators() {
		return getCache().getValidators();
	}

	/**
	 * Yields the gas station in store.
	 * 
	 * @return the gas station; this is missing if the node is not initialized yet
	 */
	protected final Optional<StorageReference> getGasStation() {
		return getCache().getGasStation();
	};

	/**
	 * Yields the versions object in store.
	 * 
	 * @return the versions object; this is missing if the node is not initialized yet
	 */
	protected final Optional<StorageReference> getVersions() {
		return getCache().getVersions();
	}

	/**
	 * Yields the current gas price.
	 * 
	 * @return the current gas price; this is missing if the node is not initialized yet
	 */
	protected final Optional<BigInteger> getGasPrice() {
		return getCache().getGasPrice();
	}

	/**
	 * Yields the current inflation.
	 * 
	 * @return the current inflation; this is missing if the node is not initialized yet
	 */
	protected final OptionalLong getInflation() {
		return getCache().getInflation();
	}

	/**
	 * Yields the hasher to use for the requests of transactions.
	 * 
	 * @return the hasher to use for the requests of transactions
	 */
	protected abstract Hasher<TransactionRequest<?>> getHasher();

	/**
	 * Verifies the signature of the given signed request.
	 * 
	 * @param request the signed request
	 * @param algorithm the signature algorithm to use
	 * @return true if and only if the signature is valid
	 * @throws StoreException is the store is misbehaving
	 * @throws UnknownReferenceException if the caller of {@code request} cannot be found in store, hence its public key cannot be recovered
	 * @throws FieldNotFoundException if the caller of {@code request} has no field for its public key, hence it is not really an account
	 */
	private boolean verifySignature(SignedTransactionRequest<?> request, SignatureAlgorithm algorithm) throws StoreException, UnknownReferenceException, FieldNotFoundException {
		try {
			return algorithm.getVerifier(getPublicKey(request.getCaller(), algorithm), SignedTransactionRequest<?>::toByteArrayWithoutSignature).verify(request, request.getSignature());
		}
		catch (InvalidKeyException | InvalidKeySpecException e) {
			LOGGER.info("the signature of " + request.getCaller() + " could not be verified since its key is invalid: " + e.getMessage());
			return false;
		}
		catch (Base64ConversionException e) {
			LOGGER.info("the signature of " + request.getCaller() + " could not be verified since its public key is not encoded in Base64 format: " + e.getMessage());
			return false;
		}
		catch (SignatureException e) {
			LOGGER.info("the signature of " + request.getCaller() + " could not be verified: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Yields the update to the given field of the object at the given reference, generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param reference the reference to the transaction; this is assumed to be a transaction of the history of {@code object}
	 * @return the update, if any; if the field of {@code object} was not modified during
	 *         the {@code transaction}, the result is empty
	 * @throws UnknownReferenceException if the response of {@code reference} cannot be found in store
	 */
	private Optional<UpdateOfField> getUpdateFromTransactionInHistory(StorageReference object, FieldSignature field, TransactionReference reference) throws UnknownReferenceException {
		if (getResponse(reference) instanceof TransactionResponseWithUpdates trwu)
			return trwu.getUpdates()
					.filter(update -> update instanceof UpdateOfField)
					.map(update -> (UpdateOfField) update)
					.filter(update -> update.getObject().equals(object) && update.getField().equals(field))
					.findFirst(); // TODO: hotspot
		else
			throw new UncheckedStoreException("Transaction reference " + reference + " belongs to the history of " + object + " but it does not contain updates");
	}

	/**
	 * Yields the public key of the given account.
	 * 
	 * @param account the account
	 * @param algorithm the signing algorithm used for the request
	 * @return the public key
	 * @throws Base64ConversionException if the public key of {@code account} is not Base64-encoded
	 * @throws InvalidKeySpecException if the public key of {@code account} is invalid for {@code algorithm}
	 * @throws StoreException if the store is misbehaving
	 * @throws FieldNotFoundException if the field holding the public key of {@code account} cannot be found, which means
	 *                                that it is not really an account
	 * @throws UnknownReferenceException if {@code account} cannot be found in store
	 */
	private PublicKey getPublicKey(StorageReference account, SignatureAlgorithm algorithm) throws Base64ConversionException, InvalidKeySpecException, UnknownReferenceException, FieldNotFoundException, StoreException {
		String publicKeyEncodedBase64 = getPublicKey(account);
		byte[] publicKeyEncoded = Base64.fromBase64String(publicKeyEncodedBase64);
		return algorithm.publicKeyFromEncoding(publicKeyEncoded);
	}

	/**
	 * Yields the outcome contained in the given transaction response.
	 * 
	 * @param response the transaction response
	 * @return the outcome
	 * @throws CodeExecutionException if the response threw an exception inside user code, that is allowed
	 *                                to be thrown by the called method or constructor
	 * @throws TransactionException if the response threw an exception outside user code, or that is not allowed
	 *                              to be thrown by the called method or constructor
	 */
	private Optional<StorageValue> getOutcome(MethodCallTransactionResponse response) throws CodeExecutionException, TransactionException {
		if (response instanceof NonVoidMethodCallTransactionSuccessfulResponse mctsr)
			return Optional.of(mctsr.getResult());
		else if (response instanceof MethodCallTransactionExceptionResponse mcter)
			throw new CodeExecutionException(mcter.getClassNameOfCause(), mcter.getMessageOfCause(), mcter.getWhere());
		else if (response instanceof MethodCallTransactionFailedResponse mctfr)
			throw new TransactionException(mctfr.getClassNameOfCause(), mctfr.getMessageOfCause(), mctfr.getWhere());
		else
			return Optional.empty(); // void methods return no value
	}

	/**
	 * Yields the value of the given string field of the given object.
	 * 
	 * @param object the object
	 * @param field the field, of string type
	 * @return the value of {@code field} of {@code object}
	 * @throws UnknownReferenceException if {@code object} cannot be found in store
	 * @throws FieldNotFoundException if {@code object} does not have the given {@code field}
	 * @throws StoreException if the store is misbehaving
	 */
	private String getStringField(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		if (getLastUpdateToField(object, field).getValue() instanceof StringValue sv)
			return sv.getValue();
		else
			throw new FieldNotFoundException(field);
	}
}