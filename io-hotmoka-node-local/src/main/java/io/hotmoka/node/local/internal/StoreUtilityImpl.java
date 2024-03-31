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

import java.math.BigInteger;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.updates.UpdateOfField;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.local.api.NodeCache;
import io.hotmoka.node.local.api.StoreUtility;
import io.hotmoka.stores.Store;

/**
 * The implementation of an object that provides methods for reconstructing data from the store of a node.
 */
public class StoreUtilityImpl implements StoreUtility {

	private final static Logger logger = Logger.getLogger(StoreUtilityImpl.class.getName());

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
	public StoreUtilityImpl(NodeInternal node) {
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
	public StoreUtilityImpl(NodeInternal node, Store store) {
		this.node = node;
		this.store = store;
	}

	@Override
	public Optional<TransactionReference> getTakamakaCodeUncommitted() {
		return getStore().getManifestUncommitted()
			.map(this::getClassTagUncommitted)
			.map(ClassTag::getJar);
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
		return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_GAS_STATION_FIELD));
	}

	@Override
	public Optional<StorageReference> getValidatorsUncommitted() {
		return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_VALIDATORS_FIELD));
	}

	@Override
	public Optional<StorageReference> getGameteUncommitted() {
		return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_GAMETE_FIELD));
	}

	@Override
	public Optional<StorageReference> getVersionsUncommitted() {
		return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_VERSIONS_FIELD));		
	}

	@Override
	public BigInteger getBalanceUncommitted(StorageReference contract) {
		return getBigIntegerFieldUncommitted(contract, FieldSignatures.BALANCE_FIELD);
	}

	@Override
	public BigInteger getRedBalanceUncommitted(StorageReference contract) {
		return getBigIntegerFieldUncommitted(contract, FieldSignatures.RED_BALANCE_FIELD);
	}

	@Override
	public BigInteger getCurrentSupplyUncommitted(StorageReference validators) {
		return getBigIntegerFieldUncommitted(validators, FieldSignatures.ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD);
	}

	@Override
	public String getPublicKeyUncommitted(StorageReference account) {
		return getStringFieldUncommitted(account, FieldSignatures.EOA_PUBLIC_KEY_FIELD);
	}

	@Override
	public StorageReference getCreatorUncommitted(StorageReference event) {
		return getReferenceFieldUncommitted(event, FieldSignatures.EVENT_CREATOR_FIELD);
	}

	@Override
	public BigInteger getNonceUncommitted(StorageReference account) {
		return getBigIntegerFieldUncommitted(account, FieldSignatures.EOA_NONCE_FIELD);
	}

	@Override
	public BigInteger getTotalBalanceUncommitted(StorageReference contract) {
		return getBalanceUncommitted(contract).add(getRedBalanceUncommitted(contract));
	}

	@Override
	public String getClassNameUncommitted(StorageReference reference) {
		return getClassTagUncommitted(reference).getClazz().getName();
	}

	@Override
	public ClassTag getClassTagUncommitted(StorageReference reference) throws NoSuchElementException {
		// we go straight to the transaction that created the object
		return node.getCaches().getResponseUncommitted(reference.getTransaction())
			.filter(response -> response instanceof TransactionResponseWithUpdates)
			.flatMap(response -> ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof ClassTag && update.getObject().equals(reference))
				.map(update -> (ClassTag) update)
				.findFirst())
			.orElseThrow(() -> new NoSuchElementException("Object " + reference + " does not exist"));
	}

	@Override
	public Stream<Update> getStateCommitted(StorageReference object) {
		Set<Update> updates = new HashSet<>();
		Stream<TransactionReference> history = getStore().getHistory(object);
		history.forEachOrdered(transaction -> addUpdatesCommitted(object, transaction, updates));
		return updates.stream();
	}

	@Override
	public Stream<UpdateOfField> getEagerFieldsUncommitted(StorageReference object) {
		Set<FieldSignature> fieldsAlreadySeen = new HashSet<>();
		NodeCache caches = node.getCaches();

		return getStore().getHistoryUncommitted(object)
			.flatMap(transaction -> enforceHasUpdates(caches.getResponseUncommitted(transaction).get()).getUpdates())
			.filter(update -> update.isEager() && update instanceof UpdateOfField && update.getObject().equals(object) &&
					fieldsAlreadySeen.add(((UpdateOfField) update).getField()))
			.map(update -> (UpdateOfField) update);
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
		return getLastUpdateUncommitted(object, field, object.getTransaction());
	}

	private static TransactionResponseWithUpdates enforceHasUpdates(TransactionResponse response) {
		if (response instanceof TransactionResponseWithUpdates)
			return (TransactionResponseWithUpdates) response;
		else
			throw new RuntimeException("Transaction " + response + " does not contain updates");
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
			if (response instanceof TransactionResponseWithUpdates trwu)
				trwu.getUpdates()
					.filter(update -> update.getObject().equals(object) && updates.stream().noneMatch(update::sameProperty))
					.forEach(updates::add);
			else
				throw new RuntimeException("Storage reference " + transaction + " does not contain updates");
		}
		catch (TransactionRejectedException e) {
			logger.log(Level.WARNING, "unexpected exception", e);
			throw new RuntimeException(e);
		}
	}

	private StorageReference getReferenceFieldUncommitted(StorageReference object, FieldSignature field) {
		return (StorageReference) getLastUpdateToFieldUncommitted(object, field).get().getValue();
	}

	private BigInteger getBigIntegerFieldUncommitted(StorageReference object, FieldSignature field) {
		return ((BigIntegerValue) getLastUpdateToFieldUncommitted(object, field).get().getValue()).getValue();
	}

	private String getStringFieldUncommitted(StorageReference object, FieldSignature field) {
		return ((StringValue) getLastUpdateToFieldUncommitted(object, field).get().getValue()).getValue();
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
			.orElseThrow(() -> new RuntimeException("Unknown transaction reference " + transaction));
	
		if (!(response instanceof TransactionResponseWithUpdates))
			throw new RuntimeException("Transaction reference " + transaction + " does not contain updates");

		return ((TransactionResponseWithUpdates) response).getUpdates()
			.filter(update -> update instanceof UpdateOfField)
			.map(update -> (UpdateOfField) update)
			.filter(update -> update.getObject().equals(object) && update.getField().equals(field))
			.findFirst();
	}
}