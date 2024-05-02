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

package io.hotmoka.stores;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.exceptions.UncheckFunction;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;

/**
 * The store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. A store is external to the node and, typically, only
 * its hash is held in the node, if consensus is needed. Stores must be thread-safe, since they can
 * be used concurrently for executing more requests.
 */
public abstract class AbstractStoreTransaction<T extends Store<T>> implements StoreTransaction<T> {
	private final static Logger LOGGER = Logger.getLogger(AbstractStoreTransaction.class.getName());
	private final T store;

	/**
	 * This lock ensures that the various components of this transaction are always aligned.
	 * For instance, histories refer to responses in store. Subclasses must take care
	 * of synchronizing on this object when they read components of this transaction, so that
	 * they are provided in a consistent state.
	 */
	private final Object lock = new Object();

	protected AbstractStoreTransaction(T store) {
		this.store = store;
	}

	public final T getStore() {
		return store;
	}

	protected final Object getLock() {
		return lock;
	}

	@Override
	public final void push(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws StoreException {
		synchronized (lock) {
			if (response instanceof TransactionResponseWithUpdates trwu) {
				setRequest(reference, request);
				setResponse(reference, response);
				expandHistory(reference, trwu);

				if (response instanceof GameteCreationTransactionResponse gctr)
					LOGGER.info(gctr.getGamete() + ": created as gamete");
			}
			else if (response instanceof InitializationTransactionResponse) {
				if (request instanceof InitializationTransactionRequest itr) {
					setRequest(reference, request);
					setResponse(reference, response);
					StorageReference manifest = itr.getManifest();
					setManifest(manifest);
					LOGGER.info(manifest + ": set as manifest");
					LOGGER.info("the node has been initialized");
				}
				else
					throw new StoreException("Trying to initialize the node with a request of class " + request.getClass().getSimpleName());
			}
			else {
				setRequest(reference, request);
				setResponse(reference, response);
			}
		}
	}

	@Override
	public final void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) throws StoreException {
		synchronized (lock) {
			setRequest(reference, request);
			setError(reference, errorMessage);
		}
	}

	@Override
	public final void replace(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws StoreException {
		synchronized (lock) {
			setResponse(reference, response);
		}
	}

	@Override
	public final Optional<TransactionReference> getTakamakaCodeUncommitted() throws StoreException {
		synchronized (lock) {
			return getManifestUncommitted()
					.map(this::getClassTagUncommitted)
					.map(ClassTag::getJar);
		}
	}

	@Override
	public final boolean nodeIsInitializedUncommitted() throws StoreException {
		return getManifestUncommitted().isPresent();
	}

	@Override
	public final Optional<StorageReference> getGasStationUncommitted() throws StoreException {
		synchronized (lock) {
			return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_GAS_STATION_FIELD));
		}
	}

	@Override
	public final Optional<StorageReference> getValidatorsUncommitted() throws StoreException {
		synchronized (lock) {
			return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_VALIDATORS_FIELD));
		}
	}

	@Override
	public final Optional<StorageReference> getGameteUncommitted() throws StoreException {
		synchronized (lock) {
			return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_GAMETE_FIELD));
		}
	}

	@Override
	public final Optional<StorageReference> getVersionsUncommitted() throws StoreException {
		synchronized (lock) {
			return getManifestUncommitted().map(_manifest -> getReferenceFieldUncommitted(_manifest, FieldSignatures.MANIFEST_VERSIONS_FIELD));		
		}
	}

	@Override
	public final BigInteger getBalanceUncommitted(StorageReference contract) {
		return getBigIntegerFieldUncommitted(contract, FieldSignatures.BALANCE_FIELD);
	}

	@Override
	public final BigInteger getRedBalanceUncommitted(StorageReference contract) {
		return getBigIntegerFieldUncommitted(contract, FieldSignatures.RED_BALANCE_FIELD);
	}

	@Override
	public final BigInteger getCurrentSupplyUncommitted(StorageReference validators) {
		return getBigIntegerFieldUncommitted(validators, FieldSignatures.ABSTRACT_VALIDATORS_CURRENT_SUPPLY_FIELD);
	}

	@Override
	public final String getPublicKeyUncommitted(StorageReference account) {
		return getStringFieldUncommitted(account, FieldSignatures.EOA_PUBLIC_KEY_FIELD);
	}

	@Override
	public final StorageReference getCreatorUncommitted(StorageReference event) {
		return getReferenceFieldUncommitted(event, FieldSignatures.EVENT_CREATOR_FIELD);
	}

	@Override
	public final BigInteger getNonceUncommitted(StorageReference account) {
		return getBigIntegerFieldUncommitted(account, FieldSignatures.EOA_NONCE_FIELD);
	}

	@Override
	public final BigInteger getTotalBalanceUncommitted(StorageReference contract) {
		return getBalanceUncommitted(contract).add(getRedBalanceUncommitted(contract));
	}

	@Override
	public final String getClassNameUncommitted(StorageReference reference) {
		return getClassTagUncommitted(reference).getClazz().getName();
	}

	@Override
	public final ClassTag getClassTagUncommitted(StorageReference reference) throws NoSuchElementException {
		// we go straight to the transaction that created the object
		try {
			synchronized (lock) {
				return getResponseUncommitted(reference.getTransaction()) // TODO: cache it?
						.filter(response -> response instanceof TransactionResponseWithUpdates)
						.flatMap(response -> ((TransactionResponseWithUpdates) response).getUpdates()
								.filter(update -> update instanceof ClassTag && update.getObject().equals(reference))
								.map(update -> (ClassTag) update)
								.findFirst())
						.orElseThrow(() -> new NoSuchElementException("Object " + reference + " does not exist"));
			}
		}
		catch (StoreException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	@Override
	public final Stream<UpdateOfField> getEagerFieldsUncommitted(StorageReference object) throws StoreException {
		var fieldsAlreadySeen = new HashSet<FieldSignature>();

		synchronized (lock) {
			return getHistoryUncommitted(object)
					.flatMap(UncheckFunction.uncheck(transaction -> enforceHasUpdates(getResponseUncommitted(transaction).get()).getUpdates())) // TODO: cache it? recheck
					.filter(update -> update.isEager() && update instanceof UpdateOfField && update.getObject().equals(object) &&
							fieldsAlreadySeen.add(((UpdateOfField) update).getField()))
					.map(update -> (UpdateOfField) update);
		}
	}

	@Override
	public final Optional<UpdateOfField> getLastUpdateToFieldUncommitted(StorageReference object, FieldSignature field) throws StoreException {
		synchronized (lock) {
			return getHistoryUncommitted(object)
					.map(transaction -> getLastUpdateUncommitted(object, field, transaction))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findFirst();
		}
	}

	@Override
	public final Optional<UpdateOfField> getLastUpdateToFinalFieldUncommitted(StorageReference object, FieldSignature field) {
		// accesses directly the transaction that created the object
		return getLastUpdateUncommitted(object, field, object.getTransaction());
	}

	/**
	 * Writes in store the given request for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param request the request
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected abstract void setRequest(TransactionReference reference, TransactionRequest<?> request) throws StoreException;

	/**
	 * Writes in store the given response for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param response the response
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected abstract void setResponse(TransactionReference reference, TransactionResponse response) throws StoreException;

	/**
	 * Writes in store the given error for the given transaction reference.
	 * 
	 * @param reference the reference of the transaction
	 * @param error the error
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected abstract void setError(TransactionReference reference, String error) throws StoreException;

	/**
	 * Sets the history of the given object, that is,
	 * the references to the transactions that provide information about
	 * its current state, in reverse chronological order (from newest to oldest).
	 * 
	 * @param object the object whose history is set
	 * @param history the stream that will become the history of the object,
	 *                replacing its previous history; this is in chronological order,
	 *                from newest transactions to oldest; hence the last transaction is
	 *                that when the object has been created
	 */
	protected abstract void setHistory(StorageReference object, Stream<TransactionReference> history) throws StoreException;

	/**
	 * Mark the node as initialized. This happens for initialization requests.
	 * 
	 * @param manifest the manifest to put in the node
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	protected abstract void setManifest(StorageReference manifest) throws StoreException;

	private static TransactionResponseWithUpdates enforceHasUpdates(TransactionResponse response) { // TODO: remove?
		if (response instanceof TransactionResponseWithUpdates trwu)
			return trwu;
		else
			throw new RuntimeException("Transaction " + response + " does not contain updates");
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
	 * Yields the update to the given field of the object at the given reference, generated during a given transaction.
	 * 
	 * @param object the reference of the object
	 * @param field the field of the object
	 * @param transaction the reference to the transaction
	 * @return the update, if any. If the field of {@code object} was not modified during
	 *         the {@code transaction}, this method returns an empty optional
	 */
	private Optional<UpdateOfField> getLastUpdateUncommitted(StorageReference object, FieldSignature field, TransactionReference transaction) {
		try {
			TransactionResponse response = getResponseUncommitted(transaction)
					.orElseThrow(() -> new StoreException("Unknown transaction reference " + transaction));

			if (response instanceof TransactionResponseWithUpdates trwu)
				return trwu.getUpdates()
						.filter(update -> update instanceof UpdateOfField)
						.map(update -> (UpdateOfField) update)
						.filter(update -> update.getObject().equals(object) && update.getField().equals(field))
						.findFirst();
			else
				throw new StoreException("Transaction reference " + transaction + " does not contain updates");
		}
		catch (StoreException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	/**
	 * Process the updates contained in the given response, expanding the history of the affected objects.
	 * 
	 * @param reference the transaction that has generated the given response
	 * @param response the response
	 * @throws StoreException if this store is not able to complete the operation correctly
	 */
	private void expandHistory(TransactionReference reference, TransactionResponseWithUpdates response) throws StoreException {
		// we collect the storage references that have been updated in the response; for each of them,
		// we fetch the list of the transaction references that affected them in the past, we add the new transaction reference
		// in front of such lists and store back the updated lists, replacing the old ones
		var modifiedObjects = response.getUpdates()
			.map(Update::getObject)
			.distinct().toArray(StorageReference[]::new);
	
		for (StorageReference object: modifiedObjects)
			setHistory(object, simplifiedHistory(object, reference, response.getUpdates()));
	}

	/**
	 * Adds the given transaction reference to the history of the given object and yields the simplified
	 * history. Simplification means that some elements of the previous history might not be useful anymore,
	 * since they get shadowed by the updates in the added transaction reference. This occurs when the values
	 * of some fields are updated in {@code added} and the useless old history element provided only values
	 * for the newly updated fields.
	 * 
	 * @param object the object whose history is being simplified
	 * @param added the transaction reference to add in front of the history of {@code object}
	 * @param addedUpdates the updates generated in {@code added}
	 * @return the simplified history, with {@code added} in front followed by a subset of {@code old}
	 */
	private Stream<TransactionReference> simplifiedHistory(StorageReference object, TransactionReference added, Stream<Update> addedUpdates) throws StoreException {
		Stream<TransactionReference> old = getHistoryUncommitted(object);

		// we trace the set of updates that are already covered by previous transactions, so that
		// subsequent history elements might become unnecessary, since they do not add any yet uncovered update
		Set<Update> covered = addedUpdates.filter(update -> update.getObject().equals(object)).collect(Collectors.toSet());
		var simplified = new ArrayList<TransactionReference>();
		simplified.add(added);
	
		var oldAsArray = old.toArray(TransactionReference[]::new);
		int length = oldAsArray.length;
		for (int pos = 0; pos < length - 1; pos++)
			addIfUncovered(oldAsArray[pos], object, covered, simplified);
	
		// the last is always useful, since it contains at least the class tag of the object
		if (length >= 1)
			simplified.add(oldAsArray[length - 1]);
	
		return simplified.stream();
	}

	/**
	 * Adds the given transaction reference to the history of the given object,
	 * if it provides updates for fields that have not yet been covered by other updates.
	 * 
	 * @param reference the transaction reference
	 * @param object the object
	 * @param covered the set of updates for the already covered fields
	 * @param history the history; this might be modified by the method, by prefixing {@code reference} at its front
	 */
	private void addIfUncovered(TransactionReference reference, StorageReference object, Set<Update> covered, List<TransactionReference> history) throws StoreException {
		Optional<TransactionResponse> maybeResponse = getResponseUncommitted(reference);

		if (maybeResponse.isEmpty())
			throw new StoreException("The history contains a reference to a transaction not in store");
		else if (maybeResponse.get() instanceof TransactionResponseWithUpdates trwu) {
			// we check if there is at least an update for a field of the object
			// that is not yet covered by another update in a previous element of the history
			Set<Update> diff = trwu.getUpdates()
				.filter(update -> update.getObject().equals(object) && covered.stream().noneMatch(update::sameProperty))
				.collect(Collectors.toSet());

			if (!diff.isEmpty()) {
				// the transaction reference actually adds at least one useful update
				history.add(reference);
				covered.addAll(diff);
			}
		}
		else
			throw new StoreException("The history contains a reference to a transaction without updates");
	}
}