package io.hotmoka.local;

import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.verification.TakamakaClassLoader;

/**
 * A class loader used to access the definition of the classes
 * of Takamaka methods or constructors executed during a transaction.
 * It adds methods that allow one to read or modify deserialized
 * storage objects in RAM during a transaction.
 */
public interface EngineClassLoader extends TakamakaClassLoader {

	/**
	 * Yields the lengths (in bytes) of the instrumented jars of the classpath and its dependencies
	 * used to create this class loader.
	 * 
	 * @return the lengths
	 */
	IntStream getLengthsOfJars();

	/**
	 * Yields the transactions that installed the jars of the classpath and its dependencies
	 * used to create this class loader.
	 * 
	 * @return the transactions
	 */
	Stream<TransactionReference> getTransactionsOfJars();

	/**
	 * Yields the transaction reference that installed the jar where the given class is defined.
	 * 
	 * @param clazz the class
	 * @return the transaction reference
	 */
	TransactionReference transactionThatInstalledJarFor(Class<?> clazz);

	/**
	 * Yields the value of the {@code storageReference} field of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	StorageReference getStorageReferenceOf(Object object);

	/**
	 * Yields the value of the boolean {@code inStorage} field of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	boolean getInStorageOf(Object object);

	/**
	 * Yields the value of the {@code balance} field of the given contract in RAM.
	 * 
	 * @param object the contract
	 * @return the value of the field
	 */
	BigInteger getBalanceOf(Object object);

	/**
	 * Yields the value of the {@code balanceRed} field of the given red/green contract in RAM.
	 * 
	 * @param object the contract
	 * @return the value of the field
	 */
	BigInteger getRedBalanceOf(Object object);

	/**
	 * Sets the value of the {@code balance} field of the given contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the balance of the contract
	 */
	void setBalanceOf(Object object, BigInteger value);

	/**
	 * Sets the value of the {@code nonce} field of the given account in RAM.
	 * 
	 * @param object the account
	 * @param value to value to set for the nonce of the account
	 */
	void setNonceOf(Object object, BigInteger value);

	/**
	 * Sets the val)ue of the {@code balanceRed} field of the given red/green contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the red balance of the contract
	 */
	void setRedBalanceOf(Object object, BigInteger value);
}