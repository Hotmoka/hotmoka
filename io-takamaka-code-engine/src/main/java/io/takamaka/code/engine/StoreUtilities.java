package io.takamaka.code.engine;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;

/**
 * An object that provides utility methods on the store of a node.
 */
public interface StoreUtilities {

	TransactionReference getTakamakaCodeUncommitted() throws NoSuchElementException;

	/**
	 * Determines if the node is initialized, that is, its manifest has been set,
	 * although possibly not yet committed.
	 * 
	 * @return true if and only if that condition holds
	 */
	boolean isInitializedUncommitted();

	/**
	 * Yields the last updates to the fields of the given object.
	 * 
	 * @param object the reference to the object
	 * @param classLoader the class loader
	 * @return the updates
	 */
	Stream<Update> getLastEagerOrLazyUpdates(StorageReference object, EngineClassLoader classLoader);

	/**
	 * Yields the gas station inside the given manifest.
	 * 
	 * @param manifest the manifest
	 * @return the gas station
	 */
	StorageReference getGasStation(StorageReference manifest);

	/**
	 * Yields the validators contract inside the given manifest.
	 * 
	 * @param manifest the manifest
	 * @return the validators contract
	 */
	StorageReference getValidators(StorageReference manifest);

	/**
	 * Yields the versions contract inside the given manifest.
	 * 
	 * @param manifest the manifest
	 * @return the versions contract
	 */
	StorageReference getVersions(StorageReference manifest);

	/**
	 * Yields the (green) balance of the given contract.
	 * 
	 * @param contract the contract
	 * @return the balance
	 */
	BigInteger getBalance(StorageReference contract);

	/**
	 * Yields the red balance of the given contract.
	 * 
	 * @param contract the contract
	 * @return the red balance
	 */
	BigInteger getRedBalance(StorageReference contract);

	/**
	 * Yields the Base64-encoded public key of the given account.
	 * 
	 * @return the public key
	 */
	String getPublicKey(StorageReference account);

	/**
	 * Yields the creator of the given event.
	 * 
	 * @param event the event
	 * @return the reference to the creator
	 */
	StorageReference getCreator(StorageReference event);

	/**
	 * Yields the nonce of the given externally owned account.
	 * 
	 * @param account the account
	 * @return the nonce
	 */
	BigInteger getNonceUncommitted(StorageReference account);

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
	 * Yields the class name of the given object, whose creation might not be committed yet.
	 * 
	 * @param reference the object
	 * @return the class name
	 * @throws NoSuchElementException if the class name cannot be determined
	 */
	String getClassNameUncommitted(StorageReference reference) throws NoSuchElementException;

	/**
	 * Yields the class tag of the given object, whose creation might not be committed yet.
	 * 
	 * @param reference the object
	 * @return the class tag
	 * @throws NoSuchElementException if the class tag cannot be determined
	 */
	ClassTag getClassTagUncommitted(StorageReference reference) throws NoSuchElementException;
}