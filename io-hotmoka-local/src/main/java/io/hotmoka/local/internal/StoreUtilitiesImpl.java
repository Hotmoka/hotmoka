package io.hotmoka.local.internal;

import java.math.BigInteger;
import java.util.HashSet;
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
import io.hotmoka.local.StoreUtilities;

/**
 * The implementation of an object that provides methods for reconstructing data from the store of a node.
 */
public class StoreUtilitiesImpl implements StoreUtilities {

	private final static Logger logger = LoggerFactory.getLogger(StoreUtilitiesImpl.class);

	/**
	 * The node whose store is accessed.
	 */
	private final NodeInternal node;

	/**
	 * Builds an object that provides utility methods on the store of a node.
	 * 
	 * @param node the node whose store is accessed
	 */
	public StoreUtilitiesImpl(NodeInternal node) {
		this.node = node;
	}

	@Override
	public Optional<TransactionReference> getTakamakaCodeUncommitted() {
		return node.getStore().getManifestUncommitted()
			.map(this::getClassTagUncommitted)
			.map(_classTag -> _classTag.jar);
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() {
		return node.getStore().getManifestUncommitted();
	}

	@Override
	public boolean nodeIsInitializedUncommitted() {
		return getManifestUncommitted().isPresent();
	}

	@Override
	public Optional<StorageReference> getGasStationUncommitted() {
		return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignature.MANIFEST_GAS_STATION_FIELD));
	}

	@Override
	public Optional<StorageReference> getValidatorsUncommitted() {
		return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignature.MANIFEST_VALIDATORS_FIELD));
	}

	@Override
	public Optional<StorageReference> getVersionsUncommitted() {
		return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignature.MANIFEST_VERSIONS_FIELD));		
	}

	@Override
	public BigInteger getBalanceUncommitted(StorageReference contract) {
		return getBigIntegerFieldUncommitted(contract, FieldSignature.BALANCE_FIELD);
	}

	@Override
	public BigInteger getRedBalanceUncommitted(StorageReference contract) {
		return getBigIntegerFieldUncommitted(contract, FieldSignature.RED_BALANCE_FIELD);
	}

	@Override
	public String getPublicKeyUncommitted(StorageReference account) {
		return getStringFieldUncommitted(account, FieldSignature.EOA_PUBLIC_KEY_FIELD);
	}

	@Override
	public StorageReference getCreatorUncommitted(StorageReference event) {
		return getReferenceFieldUncommitted(event, FieldSignature.EVENT_CREATOR_FIELD);
	}

	@Override
	public BigInteger getNonceUncommitted(StorageReference account) {
		try {
			UpdateOfField updateOfNonce = node.getStore().getHistoryUncommitted(account)
				.map(transaction -> getLastUpdateOfNonceUncommitted(account, transaction))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst().get();

			return ((BigIntegerValue) updateOfNonce.getValue()).value;
		}
		catch (Throwable t) {
			throw new InternalFailureException("could not find the last update to the nonce of " + account);
		}
	}

	@Override
	public BigInteger getTotalBalanceUncommitted(StorageReference contract, boolean isRedGreen) {
		BigInteger total = getBalanceUncommitted(contract);
		return isRedGreen ? total.add(getRedBalanceUncommitted(contract)) : total;
	}

	@Override
	public String getClassNameUncommitted(StorageReference reference) {
		return getClassTagUncommitted(reference).className;
	}

	@Override
	public ClassTag getClassTagUncommitted(StorageReference reference) {
		try {
			// we go straight to the transaction that created the object
			Optional<TransactionResponse> response = node.getStore().getResponseUncommitted(reference.transaction);
			if (!(response.get() instanceof TransactionResponseWithUpdates))
				throw new InternalFailureException("transaction reference " + reference.transaction + " does not contain updates");
	
			return ((TransactionResponseWithUpdates) response.get()).getUpdates()
				.filter(update -> update instanceof ClassTag && update.object.equals(reference))
				.map(update -> (ClassTag) update)
				.findFirst().get();
		}
		catch (Exception e) {
			logger.error("unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public Stream<Update> getStateCommitted(StorageReference object) {
		try {
			Set<Update> updates = new HashSet<>();
			Stream<TransactionReference> history = node.getStore().getHistory(object);
			history.forEachOrdered(transaction -> addUpdatesCommitted(object, transaction, updates));
			return updates.stream();
		}
		catch (Throwable t) {
			logger.error("unexpected exception", t);
			throw InternalFailureException.of(t);
		}
	}

	@Override
	public Stream<Update> getStateUncommitted(StorageReference object) {
		try {
			Set<Update> updates = new HashSet<>();
			Stream<TransactionReference> history = node.getStore().getHistoryUncommitted(object);
			history.forEachOrdered(transaction -> addUpdatesUncommitted(object, transaction, updates));
			return updates.stream();
		}
		catch (Throwable t) {
			logger.error("unexpected exception", t);
			throw InternalFailureException.of(t);
		}
	}

	/**
	 * Adds, to the given set, the updates of the eager fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param updates the set where they must be added
	 */
	private void addUpdatesUncommitted(StorageReference object, TransactionReference transaction, Set<Update> updates) {
		TransactionResponse response = node.getStore().getResponseUncommitted(transaction).get();
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new InternalFailureException("Storage reference " + object + " does not contain updates");

		((TransactionResponseWithUpdates) response).getUpdates()
			.filter(update -> update.object.equals(object) && (update instanceof ClassTag || (update instanceof UpdateOfField && update.isEager() && !isAlreadyIn((UpdateOfField) update, updates))))
			.forEach(updates::add);
	}

	private StorageReference getReferenceFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return (StorageReference) getLastUpdateToFieldUncommitted(object, field).get().getValue();
		}
		catch (Throwable t) {
			logger.error("unexpected exception", t);
			throw InternalFailureException.of(t);
		}
	}

	private BigInteger getBigIntegerFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return ((BigIntegerValue) getLastUpdateToFieldUncommitted(object, field).get().getValue()).value;
		}
		catch (Throwable t) {
			logger.error("unexpected exception", t);
			throw InternalFailureException.of(t);
		}
	}

	private String getStringFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return ((StringValue) getLastUpdateToFieldUncommitted(object, field).get().getValue()).value;
		}
		catch (Throwable t) {
			logger.error("unexpected exception", t);
			throw InternalFailureException.of(t);
		}
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
	private Optional<UpdateOfField> getLastUpdateToFieldUncommitted(StorageReference storageReference, FieldSignature field) {
		return node.getStore().getHistoryUncommitted(storageReference)
			.map(transaction -> getLastUpdateForUncommitted(storageReference, field, transaction))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();
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
	
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new InternalFailureException("transaction reference " + transaction + " does not contain updates");
	
		return ((TransactionResponseWithUpdates) response).getUpdates()
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> update.object.equals(object) && update.getField().equals(field))
			.findFirst();
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

	/**
	 * Adds, to the given set, the updates of the fields of the object at the given reference,
	 * occurred during the execution of a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param transaction the reference to the transaction
	 * @param updates the set where they must be added
	 */
	private void addUpdatesCommitted(StorageReference object, TransactionReference transaction, Set<Update> updates) {
		try {
			TransactionResponse response = node.getResponse(transaction);
			if (!(response instanceof TransactionResponseWithUpdates))
				throw new InternalFailureException("Storage reference " + transaction + " does not contain updates");

			((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof ClassTag || (update instanceof UpdateOfField && update.object.equals(object) && !isAlreadyIn((UpdateOfField) update, updates)))
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