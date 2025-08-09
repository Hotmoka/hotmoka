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
import java.util.stream.IntStream;

import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * A class loader used to access the definition of the classes
 * of Takamaka methods or constructors executed during a transaction.
 * It adds methods that allow one to read or modify deserialized
 * storage objects in RAM during a transaction.
 */
public interface EngineClassLoader extends TakamakaClassLoader {

	/**
	 * Yields the class object that represents the given storage type in the Java language.
	 * 
	 * @param type the storage type
	 * @return the class object
	 * @throws ClassNotFoundException if the class of {@code type} cannot be found
	 */
	Class<?> loadClass(StorageType type) throws ClassNotFoundException;

	/**
	 * Yields the lengths (in bytes) of the instrumented jars of the classpath and its dependencies
	 * used to create this class loader.
	 * 
	 * @return the lengths
	 */
	IntStream getLengthsOfJars();

	/**
	 * Yields the transaction reference that installed the jar where the given class is defined.
	 * 
	 * @param clazz the class
	 * @return the transaction reference, if any; this might be missing if {@code clazz} has not
	 *         been installed with any jar
	 */
	Optional<TransactionReference> transactionThatInstalledJarFor(Class<?> clazz);

	/**
	 * Determines if the given classpath includes the given dependency. That is,
	 * if that classpath is the reference of a transaction that installed a jar from
	 * which it is possible to reach the given dependency, possibly reflexively and transitively.
	 * 
	 * @param classpath the classpath
	 * @param dependency the dependency
	 * @return true if and only if that condition holds
	 */
	boolean includes(TransactionReference classpath, TransactionReference dependency);

	/**
	 * Replaces all reverified responses into the store of the node for which
	 * the class loader has been built.
	 */
	void replaceReverifiedResponses();

	/**
	 * Yields the value of the {@code storageReference} field of the given storage object in RAM.
	 * 
	 * @param object the object; this is assumed to actually correspond to an object in store
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
	 * Called at the beginning of the instrumentation of a {@code @@FromContract} method or constructor
	 * of a storage object. It forwards the call to {@code io.takamaka.code.lang.Storage.fromContract()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @throws RuntimeException in case of any possible exception thrown inside {@code io.takamaka.code.lang.Storage.fromContract()}
	 */
	void fromContract(Object callee, Object caller);

	/**
	 * Called at the beginning of the instrumentation of a payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableFromContract()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @param amount the amount of coins
	 * @throws RuntimeException in case of any possible exception thrown inside {@code io.takamaka.code.lang.Contract.fromPayableContract()}
	 */
	void payableFromContract(Object callee, Object caller, BigInteger amount);

	/**
	 * Called at the beginning of the instrumentation of a payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableFromContract()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @param amount the amount of coins
	 * @throws RuntimeException in case of any possible exception thrown inside {@code io.takamaka.code.lang.Contract.fromPayableContract()}
	 */
	void payableFromContract(Object callee, Object caller, int amount);

	/**
	 * Called at the beginning of the instrumentation of a payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableFromContract()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @param amount the amount of coins
	 * @throws RuntimeException in case of any possible exception thrown inside {@code io.takamaka.code.lang.Contract.fromPayableContract()}
	 */
	void payableFromContract(Object callee, Object caller, long amount);
}