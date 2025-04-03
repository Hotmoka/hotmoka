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

package io.hotmoka.node.tendermint.internal;

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractTrieBasedStoreTransformation;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * A transformation of a store of a Tendermint node.
 */
public class TendermintStoreTransformation extends AbstractTrieBasedStoreTransformation<TendermintNodeImpl, TendermintNodeConfig, TendermintStore, TendermintStoreTransformation> {

	/**
	 * The current validators set in this store transaction. This information could be recovered from the store transaction itself,
	 * but this field is used for caching. The validators set might be missing if the node is not initialized yet.
	 */
	private volatile Optional<TendermintValidator[]> validators;

	private final static Logger LOGGER = Logger.getLogger(TendermintStoreTransformation.class.getName());

	/**
	 * Creates a transformation whose transaction are executed with the given executors.
	 * 
	 * @param store the initial store of the transformation
	 * @param consensus the consensus to use for the execution of transactions in the transformation
	 * @param now the current time to use for the execution of transactions in the transformation
	 * @param validators the Tendermint validators at the beginning of the transformation
	 */
	protected TendermintStoreTransformation(TendermintStore store, ConsensusConfig<?,?> consensus, long now, Optional<TendermintValidator[]> validators) throws StoreException {
		super(store, consensus, now);

		this.validators = validators;
	}

	@Override
	protected StoreCache getCache() {
		return super.getCache();
	}

	protected final Optional<TendermintValidator[]> getTendermintValidators() {
		return validators;
	}

	@Override
	protected void updateCaches(TransactionResponse response, TakamakaClassLoader classLoader) throws StoreException, InterruptedException {
		super.updateCaches(response, classLoader);
	
		if (validatorsMightHaveChanged(response, classLoader)) {
			recomputeValidators();
			LOGGER.info("the validators set cache has been updated since it might have changed");
		}
	}

	/**
	 * Rewards the validators with the cost of the gas consumed for the execution of the
	 * requests delivered in this store transformation.
	 * Takes note of the gas consumed for the execution of the requests delivered
	 * in this store transformation, consequently updates the inflation and the total supply.
	 * 
	 * @param behaving the space-separated sequence of identifiers of the
	 *                 validators that behaved correctly and will be rewarded
	 * @param misbehaving the space-separated sequence of the identifiers of the validators that
	 *                    misbehaved and must be punished
	 * @throws StoreException if the store is not able to complete the operation correctly
	 * @throws InterruptedException if the current thread is interrupted before delivering the transaction
	 */
	protected final void deliverRewardTransactionToValidators(String behaving, String misbehaving) throws StoreException, InterruptedException {
		try {
			Optional<StorageReference> maybeManifest = getManifest();
			if (maybeManifest.isPresent()) {
				BigInteger gasConsumed = getGasConsumed();
				LOGGER.info("coinbase: behaving validators: " + behaving + ", misbehaving validators: " + misbehaving);
				LOGGER.info("coinbase: units of gas consumed for CPU, RAM or storage since the previous reward: " + gasConsumed);

				// we use the manifest as caller, since it is an externally-owned account
				StorageReference manifest = maybeManifest.get();
				BigInteger nonce = getNonce(manifest);
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
				BigInteger reward = getReward();
				BigInteger minted = getCoinsMinted(validators);

				var request = TransactionRequests.instanceSystemMethodCall
					(manifest, nonce, _100_000, takamakaCode, MethodSignatures.VALIDATORS_REWARD, validators,
						StorageValues.bigIntegerOf(reward), StorageValues.bigIntegerOf(minted),
						StorageValues.stringOf(behaving), StorageValues.stringOf(misbehaving),
						StorageValues.bigIntegerOf(gasConsumed), StorageValues.bigIntegerOf(deliveredCount()));
	
				TransactionResponse response = responseBuilderFor(TransactionReferences.of(getHasher().hash(request)), request).getResponseCreation().getResponse();
				// if there is only one update, it is the update of the nonce of the manifest: we prefer not to expand
				// the store with the transaction, so that the state stabilizes, which might give
				// to the underlying Tendermint engine the chance of suspending the generation of new blocks
				if (!(response instanceof TransactionResponseWithUpdates trwu) || trwu.getUpdates().count() > 1L)
					response = deliverTransaction(request);
	
				if (response instanceof MethodCallTransactionFailedResponse responseAsFailed)
					LOGGER.log(Level.SEVERE, "coinbase: could not reward the validators: " + responseAsFailed.getWhere() + ": " + responseAsFailed.getClassNameOfCause() + ": " + responseAsFailed.getMessageOfCause());
				else {
					LOGGER.info("coinbase: units of coin minted since the previous reward: " + minted);
					LOGGER.info("coinbase: units of coin rewarded to the validators for their work since the previous reward: " + reward);
				}
			}
		}
		catch (TransactionRejectedException e) {
			throw new StoreException("Could not perform the coinbase transactions", e);
		}
	}

	private void recomputeValidators() throws StoreException, InterruptedException {
		try {
			Optional<StorageReference> maybeManifest = getManifest();
			if (maybeManifest.isPresent()) {
				StorageReference manifest = maybeManifest.get();
				TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));

				StorageReference shares = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_SHARES, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_SHARES + " should not return void"))
					.asReturnedReference(MethodSignatures.GET_SHARES, StoreException::new);

				int numOfValidators = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SIZE, shares))
					.orElseThrow(() -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_SIZE + " should not return void"))
					.asReturnedInt(MethodSignatures.STORAGE_MAP_VIEW_SIZE, StoreException::new);

				var result = new TendermintValidator[numOfValidators];

				for (int num = 0; num < numOfValidators; num++) {
					StorageReference validator = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SELECT, shares, StorageValues.intOf(num)))
						.orElseThrow(() -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_SELECT + " should not return void"))
						.asReturnedReference(MethodSignatures.STORAGE_MAP_VIEW_SELECT, StoreException::new);

					String id = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.ID, validator))
						.orElseThrow(() -> new StoreException(MethodSignatures.ID + " should not return void"))
						.asReturnedString(MethodSignatures.ID, StoreException::new);

					long power = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _100_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_GET, shares, validator))
						.orElseThrow(() -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_GET + " should not return void"))
						.asReturnedBigInteger(MethodSignatures.STORAGE_MAP_VIEW_GET, StoreException::new)
						.longValueExact();

					result[num] = new TendermintValidator(id, power, getPublicKey(validator), "tendermint/PubKeyEd25519");
				}

				this.validators = Optional.of(result);
			}
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | UnknownReferenceException | FieldNotFoundException | ArithmeticException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Determines if the given response generated events of type ValidatorsUpdate triggered by validators.
	 * 
	 * @param response the response
	 * @param classLoader the class loader used for the transaction
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if some class cannot be found in the Takamaka program
	 */
	private boolean validatorsMightHaveChanged(TransactionResponse response, TakamakaClassLoader classLoader) throws StoreException {
		if (response instanceof InitializationTransactionResponse)
			return true;
		// we check if there are events of type ValidatorsUpdate triggered by the validators
		else if (response instanceof TransactionResponseWithEvents trwe && trwe.getEvents().findAny().isPresent()) {
			Optional<StorageReference> maybeManifest = getManifest();

			if (maybeManifest.isPresent()) {
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));

				return check(StoreException.class, () ->
					trwe.getEvents().filter(uncheck(StoreException.class, event -> isValidatorsUpdateEvent(event, classLoader)))
						.map(UncheckFunction.uncheck(StoreException.class, this::getCreatorOfEvent))
						.anyMatch(validators::equals));
			}
		}

		return false;
	}

	private boolean isValidatorsUpdateEvent(StorageReference event, TakamakaClassLoader classLoader) throws StoreException {
		try {
			return classLoader.isValidatorsUpdateEvent(getClassName(event));
		}
		catch (UnknownReferenceException e) {
			throw new StoreException("Event " + event + " is not an object in store", e);
		}
		catch (ClassNotFoundException e) {
			throw new StoreException(e);
		}
	}
}