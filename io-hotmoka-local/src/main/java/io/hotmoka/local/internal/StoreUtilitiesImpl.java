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

package io.hotmoka.local.internal;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

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
import io.hotmoka.local.NodeCaches;
import io.hotmoka.local.Store;
import io.hotmoka.local.StoreUtilities;

/**
 * The implementation of an object that provides methods for reconstructing data from the store of a node.
 */
public class StoreUtilitiesImpl implements StoreUtilities {

	private final static Logger logger = Logger.getLogger(StoreUtilitiesImpl.class.getName());

	/**
	 * The node whose store is accessed.
	 */
	private final NodeInternal node;

	/**
	 * The store that is accessed.
	 */
	private Store store;

	/**
	 * Builds an object that provides utility methods on the store of a node.
	 * 
	 * @param node the node whose store is accessed
	 */
	public StoreUtilitiesImpl(NodeInternal node) {
		this.node = node;
	}

	private Store getStore() {
		return store != null ? store : node.getStore();
	}

	/**
	 * Builds an object that provides utility methods on the given store.
	 * 
	 * @param node the node for which the store utilities are being built
	 * @param store the store accessed by the store utilities
	 */
	public StoreUtilitiesImpl(NodeInternal node, Store store) {
		this.node = node;
		this.store = store;
	}

	@Override
	public Optional<TransactionReference> getTakamakaCodeUncommitted() {
		return getStore().getManifestUncommitted()
			.map(this::getClassTagUncommitted)
			.map(_classTag -> _classTag.jar);
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() {
		return getStore().getManifestUncommitted();
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
	public Optional<StorageReference> getGameteUncommitted() {
		return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignature.MANIFEST_GAMETE_FIELD));
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
	public BigInteger getCurrentSupplyUncommitted(StorageReference validators) {
		return getBigIntegerFieldUncommitted(validators, FieldSignature.ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD);
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
		return getBigIntegerFieldUncommitted(account, FieldSignature.EOA_NONCE_FIELD);
	}

	@Override
	public BigInteger getTotalBalanceUncommitted(StorageReference contract) {
		return getBalanceUncommitted(contract).add(getRedBalanceUncommitted(contract));
	}

	@Override
	public String getClassNameUncommitted(StorageReference reference) {
		return getClassTagUncommitted(reference).clazz.name;
	}

	@Override
	public ClassTag getClassTagUncommitted(StorageReference reference) {
		try {
			// we go straight to the transaction that created the object
			Optional<TransactionResponse> response = node.getCaches().getResponseUncommitted(reference.transaction);
			if (!(response.get() instanceof TransactionResponseWithUpdates))
				throw new InternalFailureException("transaction reference " + reference.transaction + " does not contain updates");
	
			return ((TransactionResponseWithUpdates) response.get()).getUpdates()
				.filter(update -> update instanceof ClassTag && update.object.equals(reference))
				.map(update -> (ClassTag) update)
				.findFirst().get();
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public Stream<Update> getStateCommitted(StorageReference object) {
		try {
			Set<Update> updates = new HashSet<>();
			Stream<TransactionReference> history = getStore().getHistory(object);
			history.forEachOrdered(transaction -> addUpdatesCommitted(object, transaction, updates));
			return updates.stream();
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "unexpected exception", t);
			throw InternalFailureException.of(t);
		}
	}

	@Override
	public Stream<UpdateOfField> getEagerFieldsUncommitted(StorageReference object) {
		try {
			Set<FieldSignature> fieldsAlreadySeen = new HashSet<>();
			NodeCaches caches = node.getCaches();

			return getStore().getHistoryUncommitted(object)
				.flatMap(transaction -> enforceHasUpdates(caches.getResponseUncommitted(transaction).get()).getUpdates())
				.filter(update -> update.isEager() && update instanceof UpdateOfField && update.object.equals(object) &&
						fieldsAlreadySeen.add(((UpdateOfField) update).getField()))
				.map(update -> (UpdateOfField) update);
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "unexpected exception", t);
			throw InternalFailureException.of(t);
		}
	}

	@Override
	public Optional<UpdateOfField> getLastUpdateToFieldUncommitted(StorageReference object, FieldSignature field) {
		return getStore().getHistoryUncommitted(object)
			.map(transaction -> getLastUpdateUncommitted(object, field, transaction))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();
	}

	@Override
	public Optional<UpdateOfField> getLastUpdateToFinalFieldUncommitted(StorageReference object, FieldSignature field) {
		// accesses directly the transaction that created the object
		return getLastUpdateUncommitted(object, field, object.transaction);
	}

	private static TransactionResponseWithUpdates enforceHasUpdates(TransactionResponse response) {
		if (response instanceof TransactionResponseWithUpdates)
			return (TransactionResponseWithUpdates) response;
		else
			throw new InternalFailureException("Transaction " + response + " does not contain updates");
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
				.filter(update -> update.object.equals(object) && updates.stream().noneMatch(update::sameProperty))
				.forEach(updates::add);
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "unexpected exception", e);
			throw InternalFailureException.of(e);
		}
	}

	private StorageReference getReferenceFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return (StorageReference) getLastUpdateToFieldUncommitted(object, field).get().getValue();
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "unexpected exception", t);
			throw InternalFailureException.of(t);
		}
	}

	private BigInteger getBigIntegerFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return ((BigIntegerValue) getLastUpdateToFieldUncommitted(object, field).get().getValue()).value;
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "unexpected exception", t);
			throw InternalFailureException.of(t);
		}
	}

	private String getStringFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return ((StringValue) getLastUpdateToFieldUncommitted(object, field).get().getValue()).value;
		}
		catch (Throwable t) {
			logger.log(Level.WARNING, "unexpected exception", t);
			throw InternalFailureException.of(t);
		}
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
	private Optional<UpdateOfField> getLastUpdateUncommitted(StorageReference object, FieldSignature field, TransactionReference transaction) {
		TransactionResponse response = node.getCaches().getResponseUncommitted(transaction)
			.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + transaction));
	
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new InternalFailureException("transaction reference " + transaction + " does not contain updates");

		return ((TransactionResponseWithUpdates) response).getUpdates()
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> update.object.equals(object) && update.getField().equals(field))
			.findFirst();
	}
}