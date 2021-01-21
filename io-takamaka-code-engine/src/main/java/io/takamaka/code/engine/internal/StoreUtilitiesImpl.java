package io.takamaka.code.engine.internal;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfField;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.DeserializationError;
import io.takamaka.code.engine.AbstractLocalNode;
import io.takamaka.code.engine.StoreUtilities;

/**
 * An object that provides utility methods on the store of a node.
 */
public class StoreUtilitiesImpl implements StoreUtilities {

	private final static Logger logger = LoggerFactory.getLogger(StoreUtilitiesImpl.class);

	/**
	 * The node whose store is accessed.
	 */
	private final AbstractLocalNode<?,?> node;

	/**
	 * Builds an object that provides utility methods on the store of a node.
	 * 
	 * @param node the node whose store is accessed
	 */
	public StoreUtilitiesImpl(AbstractLocalNode<?,?> node) {
		this.node = node;
	}

	@SuppressWarnings("resource")
	public TransactionReference getTakamakaCodeUncommitted() throws NoSuchElementException {
		return getClassTagUncommitted(node.getStore().getManifestUncommitted().get()).jar;
	}

	@Override
	public boolean isInitializedUncommitted() {
		return node.getStore().getManifestUncommitted().isPresent();
	}

	/**
	 * Yields the most recent update for the given field
	 * of the object with the given storage reference.
	 * If this node has some form of commit, the last update might
	 * not necessarily be already committed.
	 * 
	 * @param storageReference the storage reference
	 * @param field the field whose update is being looked for
	 * @return the update
	 */
	private UpdateOfField getLastUpdateToFieldUncommitted(StorageReference storageReference, FieldSignature field) {
		return node.getStore().getHistoryUncommitted(storageReference)
			.map(transaction -> getLastUpdateForUncommitted(storageReference, field, transaction))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst().orElseThrow(() -> new DeserializationError("did not find the last update for " + field + " of " + storageReference));
	}

	@Override
	public Stream<Update> getLastEagerOrLazyUpdates(StorageReference object) {
		TransactionResponse response = getResponseUncommitted(object.transaction);
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new DeserializationError("Storage reference " + object + " does not contain updates");
	
		return collectUpdatesFor(object);
	}

	/**
	 * Yields the response generated for the request with the given reference.
	 * It is guaranteed that the transaction has been already successfully delivered,
	 * hence a response must exist in store.
	 * 
	 * @param reference the reference of the transaction, possibly not yet committed
	 * @return the response of the transaction
	 */
	private final TransactionResponse getResponseUncommitted(TransactionReference reference) {
		return node.getStore().getResponseUncommitted(reference)
			.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + reference));
	}

	/**
	 * Yields the update to the given field of the object at the given reference,
	 * generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param transaction the reference to the transaction
	 * @return the update, if any. If the field of {@code object} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdateForUncommitted(StorageReference object, FieldSignature field, TransactionReference transaction) {
		TransactionResponse response = node.getStore().getResponseUncommitted(transaction)
			.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + transaction));

		if (response instanceof TransactionResponseWithUpdates)
			return ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(object) && update.getField().equals(field))
				.findFirst();
	
		return Optional.empty();
	}

	/**
	 * Yields the update to the nonce of the given account, generated during a given transaction.
	 * 
	 * @param account the reference of the account
	 * @param transaction the reference to the transaction
	 * @return the update to the nonce, if any. If the nonce of {@code account} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdateOfNonceUncommitted(StorageReference account, TransactionReference transaction) {
		TransactionResponse response = node.getStore().getResponseUncommitted(transaction)
			.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + transaction));

		if (response instanceof TransactionResponseWithUpdates)
			return ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.filter(update -> update.object.equals(account) && (update.getField().equals(FieldSignature.EOA_NONCE_FIELD) || update.getField().equals(FieldSignature.RGEOA_NONCE_FIELD)))
				.findFirst();
	
		return Optional.empty();
	}

	@Override
	public StorageReference getGasStation(StorageReference manifest) {
		return (StorageReference) getLastUpdateToFieldUncommitted(manifest, FieldSignature.MANIFEST_GAS_STATION_FIELD).getValue();
	}

	@Override
	public StorageReference getValidators(StorageReference manifest) {
		return (StorageReference) getLastUpdateToFieldUncommitted(manifest, FieldSignature.MANIFEST_VALIDATORS_FIELD).getValue();
	}

	@Override
	public StorageReference getVersions(StorageReference manifest) {
		return (StorageReference) getLastUpdateToFieldUncommitted(manifest, FieldSignature.MANIFEST_VERSIONS_FIELD).getValue();
	}

	@Override
	public BigInteger getBalance(StorageReference contract) {
		return ((BigIntegerValue) getLastUpdateToFieldUncommitted(contract, FieldSignature.BALANCE_FIELD).getValue()).value;
	}

	@Override
	public BigInteger getRedBalance(StorageReference contract) {
		return ((BigIntegerValue) getLastUpdateToFieldUncommitted(contract, FieldSignature.RED_BALANCE_FIELD).getValue()).value;
	}

	@Override
	public String getPublicKey(StorageReference account) {
		return ((StringValue) getLastUpdateToFieldUncommitted(account, FieldSignature.EOA_PUBLIC_KEY_FIELD).getValue()).value;
	}

	@Override
	public StorageReference getCreator(StorageReference event) {
		return (StorageReference) getLastUpdateToFieldUncommitted(event, FieldSignature.EVENT_CREATOR_FIELD).getValue();
	}

	@Override
	public BigInteger getNonceUncommitted(StorageReference account) {
		UpdateOfField updateOfNonce = node.getStore().getHistoryUncommitted(account)
			.map(transaction -> getLastUpdateOfNonceUncommitted(account, transaction))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst()
			.orElseThrow(() -> new DeserializationError("did not find the last update to the nonce of " + account));

		return ((BigIntegerValue) updateOfNonce.getValue()).value;
	}

	@Override
	public BigInteger getTotalBalanceUncommitted(StorageReference contract, boolean isRedGreen) {
		BigInteger total = getBalance(contract);
		if (isRedGreen)
			total = total.add(getRedBalance(contract));

		return total;
	}

	@Override
	public String getClassNameUncommitted(StorageReference reference) throws NoSuchElementException {
		return getClassTagUncommitted(reference).className;
	}

	@Override
	public ClassTag getClassTagUncommitted(StorageReference reference) throws NoSuchElementException {
		try {
			// we go straight to the transaction that created the object
			Optional<TransactionResponse> response = node.getStore().getResponseUncommitted(reference.transaction);
			if (response.isEmpty())
				throw new NoSuchElementException("unknown transaction reference " + reference.transaction);
	
			if (!(response.get() instanceof TransactionResponseWithUpdates))
				throw new NoSuchElementException("transaction reference " + reference.transaction + " does not contain updates");
	
			return ((TransactionResponseWithUpdates) response.get()).getUpdates()
				.filter(update -> update instanceof ClassTag && update.object.equals(reference))
				.map(update -> (ClassTag) update)
				.findFirst().get();
		}
		catch (NoSuchElementException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Adds, to the given set, all the latest updates to the fields of the
	 * object at the given storage reference.
	 * 
	 * @param object the storage reference
	 */
	private Stream<Update> collectUpdatesFor(StorageReference object) {
		Set<Update> updates = new HashSet<>();
		Stream<TransactionReference> history = node.getStore().getHistory(object);
		// scans the history of the object; there is no reason to look beyond the total number of fields whose update was expected to be found
		history.forEachOrdered(transaction -> addUpdatesFor(object, transaction, updates));
		return updates.stream();
	}

	/**
	 * Adds, to the given set, the updates of the fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param updates the set where they must be added
	 */
	private void addUpdatesFor(StorageReference object, TransactionReference transaction, Set<Update> updates) {
		try {
			TransactionResponse response = node.getResponse(transaction);
			if (response instanceof TransactionResponseWithUpdates)
				((TransactionResponseWithUpdates) response).getUpdates()
					.filter(update -> update instanceof ClassTag ||
						(update instanceof UpdateOfField && update.object.equals(object) && !isAlreadyIn((UpdateOfField) update, updates)))
					.forEach(updates::add);
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * Determines if the given set of updates contains an update for the
	 * same object and field as the given update.
	 * 
	 * @param update the given update
	 * @param updates the set
	 * @return true if and only if that condition holds
	 */
	private static boolean isAlreadyIn(UpdateOfField update, Set<Update> updates) {
		FieldSignature field = update.getField();
		return updates.stream()
			.filter(_update -> _update instanceof UpdateOfField)
			.map(_update -> (UpdateOfField) _update)
			.map(UpdateOfField::getField)
			.anyMatch(field::equals);
	}
}