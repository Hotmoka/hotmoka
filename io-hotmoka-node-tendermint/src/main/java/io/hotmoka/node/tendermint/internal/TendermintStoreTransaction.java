package io.hotmoka.node.tendermint.internal;

import static io.hotmoka.exceptions.CheckSupplier.check;
import static io.hotmoka.exceptions.UncheckPredicate.uncheck;

import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.AbstractTrieBasedStoreTransaction;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.xodus.env.Transaction;

public class TendermintStoreTransaction extends AbstractTrieBasedStoreTransaction<TendermintStore> {

	/**
	 * The current validators set in this store transaction. This information could be recovered from the store
	 * transaction itself, but this field is used for caching. The validators set might be missing if the
	 * node is not initialized yet.
	 */
	private volatile Optional<TendermintValidator[]> validators;

	private final static Logger LOGGER = Logger.getLogger(TendermintStoreTransaction.class.getName());

	protected TendermintStoreTransaction(TendermintStore store, ConsensusConfig<?,?> consensus, long now, Transaction txn) throws StoreException {
		super(store, consensus, now, txn);
	}

	@Override
	protected void setRequest(TransactionReference reference, TransactionRequest<?> request) throws StoreException {
		// nothing to do, since Tendermint keeps requests inside its blockchain
	}

	@Override
	protected void setError(TransactionReference reference, String error) throws StoreException {
		// nothing to do, since Tendermint keeps error messages inside the blockchain, in the field "data" of its transactions
	}

	public final Optional<TendermintValidator[]> getTendermintValidatorsUncommitted() throws StoreException {
		if (validators.isEmpty())
			recomputeValidators();

		return validators;
	}

	private static final BigInteger _50_000 = BigInteger.valueOf(50_000);
	private static final MethodSignature SIZE = MethodSignatures.ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "size", StorageTypes.INT);
	private static final MethodSignature GET_SHARES = MethodSignatures.ofNonVoid(StorageTypes.VALIDATORS, "getShares", StorageTypes.STORAGE_MAP_VIEW);
	private static final MethodSignature SELECT = MethodSignatures.ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "select", StorageTypes.OBJECT, StorageTypes.INT);
	private static final MethodSignature GET = MethodSignatures.ofNonVoid(StorageTypes.STORAGE_MAP_VIEW, "get", StorageTypes.OBJECT, StorageTypes.OBJECT);

	private void recomputeValidators() throws StoreException {
		try {
			Optional<StorageReference> maybeManifest = getManifestUncommitted();
			if (maybeManifest.isPresent()) {
				StorageReference manifest = maybeManifest.get();
				TransactionReference takamakaCode = getTakamakaCodeUncommitted().orElseThrow(() -> new StoreException("The manifest is set but the Takamaka code reference is not set"));
				StorageReference validators = getValidatorsUncommitted().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));

				var shares = (StorageReference) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall // TODO: check casts
					(manifest, _50_000, takamakaCode, GET_SHARES, validators))
					.orElseThrow(() -> new StoreException(GET_SHARES + " should not return void"));

				int numOfValidators = ((IntValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _50_000, takamakaCode, SIZE, shares))
					.orElseThrow(() -> new StoreException(SIZE + " should not return void"))).getValue();

				var result = new TendermintValidator[numOfValidators];

				for (int num = 0; num < numOfValidators; num++) {
					var validator = (StorageReference) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _50_000, takamakaCode, SELECT, shares, StorageValues.intOf(num)))
						.orElseThrow(() -> new StoreException(SELECT + " should not return void"));

					String id = ((StringValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _50_000, takamakaCode, MethodSignatures.ID, validator))
						.orElseThrow(() -> new StoreException(MethodSignatures.ID + " should not return void"))).getValue();

					long power = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
						(manifest, _50_000, takamakaCode, GET, shares, validator))
						.orElseThrow(() -> new StoreException(GET + " should not return void"))).getValue().longValue();

					String publicKey = getPublicKeyUncommitted(validator);

					result[num] = new TendermintValidator(id, power, publicKey, "tendermint/PubKeyEd25519");
				}
			}
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new StoreException(e);
		}
	}

	@Override
	protected void invalidateCachesIfNeeded(TransactionResponse response, EngineClassLoader classLoader) throws StoreException {
		super.invalidateCachesIfNeeded(response, classLoader);
	
		if (validatorsMightHaveChanged(response, classLoader)) {
			LOGGER.info("the validators set might have changed: deleting their cache");
			validators = Optional.empty();
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
			Optional<StorageReference> maybeManifest = getManifestUncommitted();

			if (maybeManifest.isPresent()) {
				StorageReference validators = getValidatorsUncommitted().orElseThrow(() -> new StoreException("The manifest is set but the validators are not set"));
				Stream<StorageReference> events = trwe.getEvents();

				return check(StoreException.class, () ->
					events.filter(uncheck(event -> isValidatorsUpdateEvent(event, classLoader)))
					.map(this::getCreatorUncommitted)
					.anyMatch(validators::equals)
				);
			}
		}

		return false;
	}

	private boolean isValidatorsUpdateEvent(StorageReference event, EngineClassLoader classLoader) throws StoreException {
		try {
			return classLoader.isValidatorsUpdateEvent(getClassNameUncommitted(event));
		}
		catch (ClassNotFoundException e) {
			throw new StoreException(e);
		}
	}
}