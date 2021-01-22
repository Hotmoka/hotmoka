package io.takamaka.code.engine;

import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * An object that provides methods for reconstructing data from the store of a node.
 * Most methods refer to the uncommitted store, that is, to the store including
 * previous transactions that have not been committed yet.
 * Others refer to the committed state instead. If the node has no notion of commit,
 * the semantics of both kinds of methods coincide.
 */
public interface StoreUtilities {

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
	 * @param isRedGreen true if and only if the {@code contract} has both a red and a green balance;
	 *                   otherwise, it means that it has only the green balance
	 * @return the total balance
	 */
	BigInteger getTotalBalanceUncommitted(StorageReference contract, boolean isRedGreen);

	/**
	 * Yields the Base64-encoded public key of the given account.
	 * 
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
	 * Yields the nonce of the given externally owned account (normal or red/green).
	 * 
	 * @param account the account
	 * @return the nonce
	 */
	BigInteger getNonceUncommitted(StorageReference account);

	/**
	 * Yields the class name of the given object, whose creation might not be committed yet.
	 * 
	 * @param reference the object
	 * @return the class name
	 */
	String getClassNameUncommitted(StorageReference reference);

	/**
	 * Yields the class tag of the given object, whose creation might not be committed yet.
	 * 
	 * @param reference the object
	 * @return the class tag
	 */
	ClassTag getClassTagUncommitted(StorageReference reference);

	/**
	 * Yields the uncommitted state of the given object, that is, the last updates, possibly still uncommitted, for its fields.
	 * 
	 * @param object the reference to the object
	 * @return the state
	 */
	Stream<Update> getStateUncommitted(StorageReference object);

	/**
	 * Yields the committed state of the given object, that is, the last updates committed for its fields.
	 * 
	 * @param object the reference to the object
	 * @return the state
	 */
	Stream<Update> getStateCommitted(StorageReference object);
}