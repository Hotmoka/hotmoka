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
import java.util.NoSuchElementException;
import java.util.Optional;

import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.hotmoka.node.local.api.StoreUtility;
import io.hotmoka.stores.StoreException;

/**
 * The implementation of an object that provides methods for reconstructing data from the store of a node.
 */
public class StoreUtilityImpl implements StoreUtility {

	/**
	 * The node whose store is accessed.
	 */
	private final AbstractLocalNodeImpl<?, ?> node;

	/**
	 * Builds an object that provides utility methods on the store of a node.
	 * 
	 * @param node the node whose store is accessed
	 */
	public StoreUtilityImpl(AbstractLocalNodeImpl<?, ?> node) {
		this.node = node;
	}

	@Override
	public boolean nodeIsInitializedUncommitted() throws StoreException {
		return node.getManifestUncommitted().isPresent();
	}

	@Override
	public Optional<StorageReference> getGasStationUncommitted() throws StoreException {
		return node.getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_GAS_STATION_FIELD));
	}

	@Override
	public Optional<StorageReference> getValidatorsUncommitted() throws StoreException {
		return node.getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_VALIDATORS_FIELD));
	}

	@Override
	public Optional<StorageReference> getGameteUncommitted() throws StoreException {
		return node.getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_GAMETE_FIELD));
	}

	@Override
	public Optional<StorageReference> getVersionsUncommitted() throws StoreException {
		return node.getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_VERSIONS_FIELD));		
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
	public String getClassNameUncommitted(StorageReference reference) {
		return getClassTagUncommitted(reference).getClazz().getName();
	}

	@Override
	public ClassTag getClassTagUncommitted(StorageReference reference) throws NoSuchElementException {
		// we go straight to the transaction that created the object
		return node.caches.getResponseUncommitted(reference.getTransaction())
			.filter(response -> response instanceof TransactionResponseWithUpdates)
			.flatMap(response -> ((TransactionResponseWithUpdates) response).getUpdates()
				.filter(update -> update instanceof ClassTag && update.getObject().equals(reference))
				.map(update -> (ClassTag) update)
				.findFirst())
			.orElseThrow(() -> new NoSuchElementException("Object " + reference + " does not exist"));
	}

	private Optional<UpdateOfField> getLastUpdateToFieldUncommitted(StorageReference object, FieldSignature field) throws StoreException {
		return node.getHistoryUncommitted(object)
			.map(transaction -> getLastUpdateUncommitted(object, field, transaction))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();
	}

	private StorageReference getReferenceFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return (StorageReference) getLastUpdateToFieldUncommitted(object, field).get().getValue();
		}
		catch (StoreException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	private BigInteger getBigIntegerFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return ((BigIntegerValue) getLastUpdateToFieldUncommitted(object, field).get().getValue()).getValue();
		}
		catch (StoreException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	private String getStringFieldUncommitted(StorageReference object, FieldSignature field) {
		try {
			return ((StringValue) getLastUpdateToFieldUncommitted(object, field).get().getValue()).getValue();
		}
		catch (StoreException e) {
			throw new RuntimeException(e); // TODO
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
		TransactionResponse response = node.caches.getResponseUncommitted(transaction)
			.orElseThrow(() -> new RuntimeException("Unknown transaction reference " + transaction));
	
		if (response instanceof TransactionResponseWithUpdates trwu)
			return trwu.getUpdates()
					.filter(update -> update instanceof UpdateOfField)
					.map(update -> (UpdateOfField) update)
					.filter(update -> update.getObject().equals(object) && update.getField().equals(field))
					.findFirst();
		else
			throw new RuntimeException("Transaction reference " + transaction + " does not contain updates");
	}
}