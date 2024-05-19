package io.hotmoka.node.tendermint.internal;

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractTrieBasedStoreTransaction;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.StoreException;

public class TendermintStoreTransaction extends AbstractTrieBasedStoreTransaction<TendermintStore, TendermintStoreTransaction> {

	/**
	 * The current validators set in this store transaction. This information could be recovered from the store transaction itself,
	 * but this field is used for caching. The validators set might be missing if the node is not initialized yet.
	 */
	private volatile Optional<TendermintValidator[]> validators;

	private final static Logger LOGGER = Logger.getLogger(TendermintStoreTransaction.class.getName());

	protected TendermintStoreTransaction(TendermintStore store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now, Optional<TendermintValidator[]> validators) throws StoreException {
		super(store, executors, consensus, now);

		this.validators = validators;
	}

	protected final Optional<TendermintValidator[]> getTendermintValidators() {
		return validators;
	}

	@Override
	protected void invalidateCachesIfNeeded(TransactionResponse response, EngineClassLoader classLoader) throws StoreException {
		super.invalidateCachesIfNeeded(response, classLoader);
	
		if (validatorsMightHaveChanged(response, classLoader)) {
			recomputeValidators();
			LOGGER.info("the validators set cache has been updated since it might have changed");
		}
	}

	private void recomputeValidators() throws StoreException {
		try {
			Optional<StorageReference> maybeManifest = getManifest();
			if (maybeManifest.isPresent()) {
				StorageReference manifest = maybeManifest.get();
				TransactionReference takamakaCode = getTakamakaCode().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				BigInteger _50_000 = BigInteger.valueOf(50_000);

				StorageReference shares = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _50_000, takamakaCode, MethodSignatures.GET_SHARES, validators))
					.orElseThrow(() -> new StoreException(MethodSignatures.GET_SHARES + " should not return void"))
					.asReference(value -> new StoreException(MethodSignatures.GET_SHARES + " should return a reference, not a " + value.getClass().getName()));

				int numOfValidators = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _50_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SIZE, shares))
					.orElseThrow(() -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_SIZE + " should not return void"))
					.asInt(value -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_SIZE + " should return an integer, not a " + value.getClass().getName()));

				var result = new TendermintValidator[numOfValidators];

				for (int num = 0; num < numOfValidators; num++) {
					StorageReference validator = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _50_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_SELECT, shares, StorageValues.intOf(num)))
						.orElseThrow(() -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_SELECT + " should not return void"))
						.asReference(value -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_SELECT + " should return a reference, not a " + value.getClass().getName()));

					String id = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _50_000, takamakaCode, MethodSignatures.ID, validator))
						.orElseThrow(() -> new StoreException(MethodSignatures.ID + " should not return void"))
						.asString(value -> new StoreException(MethodSignatures.ID + " should return a string, not a " + value.getClass().getName()));

					long power = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _50_000, takamakaCode, MethodSignatures.STORAGE_MAP_VIEW_GET, shares, validator))
						.orElseThrow(() -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_GET + " should not return void"))
						.asBigInteger(value -> new StoreException(MethodSignatures.STORAGE_MAP_VIEW_GET + " should return a BigInteger, not a " + value.getClass().getName()))
						.longValue();

					result[num] = new TendermintValidator(id, power, getPublicKey(validator), "tendermint/PubKeyEd25519");
				}

				this.validators = Optional.of(result);
			}
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException | UnknownReferenceException | FieldNotFoundException e) {
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
	private boolean validatorsMightHaveChanged(TransactionResponse response, EngineClassLoader classLoader) throws StoreException {
		if (response instanceof InitializationTransactionResponse)
			return true;
		// we check if there are events of type ValidatorsUpdate triggered by the validators
		else if (response instanceof TransactionResponseWithEvents trwe && trwe.getEvents().findAny().isPresent()) {
			Optional<StorageReference> maybeManifest = getManifest();

			if (maybeManifest.isPresent()) {
				StorageReference validators = getValidators().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				Stream<StorageReference> events = trwe.getEvents();

				try {
					return check(StoreException.class, UnknownReferenceException.class, FieldNotFoundException.class, () ->
						events.filter(uncheck(event -> isValidatorsUpdateEvent(event, classLoader)))
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

	private boolean isValidatorsUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws StoreException {
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