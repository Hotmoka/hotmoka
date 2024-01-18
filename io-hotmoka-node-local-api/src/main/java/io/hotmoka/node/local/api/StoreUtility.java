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

package io.hotmoka.node.local.api;

import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.updates.UpdateOfField;
import io.hotmoka.beans.api.values.StorageReference;

/**
 * An object that provides methods for reconstructing data from the store of a node.
 * Most methods refer to the uncommitted store, that is, to the store including
 * previous transactions that have not been committed yet.
 * Others refer to the committed state instead. If the node has no notion of commit,
 * the semantics of both kinds of methods coincide.
 */
public interface StoreUtility {

	/**
	 * Determines if the node is initialized, that is, its manifest has been set,
	 * although possibly not yet committed.
	 * 
	 * @return true if and only if that condition holds
	 */
	boolean nodeIsInitializedUncommitted();

	/**
	 * Yields the reference to the transaction, possibly not yet committed,
	 * that has installed the Takamaka base classes in the store of the node.
	 * 
	 * @return the reference, if any
	 */
	Optional<TransactionReference> getTakamakaCodeUncommitted();

	/**
	 * Yields the manifest of the node, if the latter is already initialized.
	 * 
	 * @return the manifest, if any
	 */
	Optional<StorageReference> getManifestUncommitted();

	/**
	 * Yields the gas station inside the manifest of the node, if the latter is already initialized.
	 * 
	 * @return the gas station, if any
	 */
	Optional<StorageReference> getGasStationUncommitted();

	/**
	 * Yields the validators contract inside the manifest of the node, if the latter is already initialized.
	 * 
	 * @return the validators contract, if any
	 */
	Optional<StorageReference> getValidatorsUncommitted();

	/**
	 * Yields the versions contract inside the manifest of the node, if the latter is already initialized.
	 * 
	 * @return the versions contract, if any
	 */
	Optional<StorageReference> getVersionsUncommitted();

	/**
	 * Yields the gamete account of the node, if the latter is already initialized.
	 * 
	 * @return the gamete account, if any
	 */
	Optional<StorageReference> getGameteUncommitted();

	/**
	 * Yields the (green) balance of the given (normal or red/green) contract.
	 * 
	 * @param contract the contract
	 * @return the balance
	 */
	BigInteger getBalanceUncommitted(StorageReference contract);

	/**
	 * Yields the red balance of the given red/green contract.
	 * 
	 * @param contract the contract
	 * @return the red balance
	 */
	BigInteger getRedBalanceUncommitted(StorageReference contract);

	/**
	 * Yields the total balance of the given contract (green plus red, if any).
	 * 
	 * @param contract the contract
	 * @return the total balance
	 */
	BigInteger getTotalBalanceUncommitted(StorageReference contract);

	/**
	 * Yields the Base64-encoded public key of the given account.
	 * 
	 * @param account the account
	 * @return the public key
	 */
	String getPublicKeyUncommitted(StorageReference account);

	/**
	 * Yields the creator of the given event.
	 * 
	 * @param event the event
	 * @return the reference to the creator
	 */
	StorageReference getCreatorUncommitted(StorageReference event);

	/**
	 * Yields the nonce of the given externally owned account.
	 * 
	 * @param account the account
	 * @return the nonce
	 */
	BigInteger getNonceUncommitted(StorageReference account);

	/**
	 * Yields the current supply of coins of the given validators object.
	 * 
	 * @param validators the validators object
	 * @return the current supply
	 */
	BigInteger getCurrentSupplyUncommitted(StorageReference validators);

	/**
	 * Yields the class name of the given object, whose creation might not be committed yet.
	 * 
	 * @param object the object
	 * @return the class name
	 */
	String getClassNameUncommitted(StorageReference object);

	/**
	 * Yields the class tag of the given object, whose creation might not be committed yet.
	 * 
	 * @param object the object
	 * @return the class tag
	 */
	ClassTag getClassTagUncommitted(StorageReference object);

	/**
	 * Yields the uncommitted eager fields of the given object, that is, their last updates, possibly still uncommitted.
	 * 
	 * @param object the reference to the object
	 * @return the last updates to the eager fields of {@code object}
	 */
	Stream<UpdateOfField> getEagerFieldsUncommitted(StorageReference object);

	/**
	 * Yields the committed state of the given object, that is, the last updates committed for its fields.
	 * 
	 * @param object the reference to the object
	 * @return the state
	 */
	Stream<Update> getStateCommitted(StorageReference object);

	/**
	 * Yields the most recent update to the given field
	 * of the object with the given storage reference.
	 * If this node has some form of commit, this last update might
	 * not necessarily be already committed.
	 * 
	 * @param object the storage reference of the object
	 * @param field the field whose update is being looked for
	 * @return the update, if any
	 */
	Optional<UpdateOfField> getLastUpdateToFieldUncommitted(StorageReference object, FieldSignature field);

	/**
	 * Yields the most recent update for the given {@code final} field
	 * of the object with the given storage reference.
	 * If this node has some form of commit, the last update might
	 * not necessarily be already committed.
	 * Its implementation can be identical to
	 * that of {@link #getLastUpdateToFieldUncommitted(StorageReference, FieldSignature)},
	 * or instead exploit the fact that the field is {@code final}, for an optimized look-up.
	 * 
	 * @param object the storage reference
	 * @param field the field whose update is being looked for
	 * @return the update, if any
	 */
	Optional<UpdateOfField> getLastUpdateToFinalFieldUncommitted(StorageReference object, FieldSignature field);
}