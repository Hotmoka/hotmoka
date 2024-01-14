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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.verification.api.TakamakaClassLoader;

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
	 * Sets the value of the {@code balanceRed} field of the given red/green contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the red balance of the contract
	 */
	void setRedBalanceOf(Object object, BigInteger value);

	/**
	 * Adds the given amount of coins to the current amount of the validators set of the node.
	 * 
	 * @param validators the object containing the validators set
	 * @param amount the amount to add (if positive) or remove (if negative)
	 */
	void increaseCurrentSupply(Object validators, BigInteger amount);
}