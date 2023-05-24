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

package io.hotmoka.verification.api;

import io.hotmoka.whitelisting.api.ResolvingClassLoader;

/**
 * A class loader used to access the definition of the classes of a Takamaka program.
 */
public interface TakamakaClassLoader extends ResolvingClassLoader {

	/**
	 * Determines if a class is an instance of the storage class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that class extends {@code io.takamaka.code.lang.Storage}
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isStorage(String className) throws ClassNotFoundException;

	/**
	 * Checks if a class is an instance of the contract class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isContract(String className) throws ClassNotFoundException;

	/**
	 * Checks if a class is an instance of the consensus update event class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isConsensusUpdateEvent(String className) throws ClassNotFoundException;

	/**
	 * Checks if a class is an instance of the gas price update event class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isGasPriceUpdateEvent(String className) throws ClassNotFoundException;

	/**
	 * Checks if a class is an instance of the inflation update event class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isInflationUpdateEvent(String className) throws ClassNotFoundException;

	/**
	 * Checks if a class is an instance of the validators update event class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isValidatorsUpdateEvent(String className) throws ClassNotFoundException;

	/**
	 * Checks if a class is an instance of another class.
	 * 
	 * @param className the class
	 * @param superclassName the potential superclass of {@code className}
	 * @return true if and only if {@code className} is a subclass of {@code superclassName}
	 * @throws ClassNotFoundException if {@code className} or {@code superclassName} cannot be found in the Takamaka program
	 */
	boolean isa(String className, String superclassName) throws ClassNotFoundException;

	/**
	 * Checks if a class is annotated as {@code @@Exported}.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isExported(String className) throws ClassNotFoundException;

	/**
	 * Checks if a class is actually an interface.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isInterface(String className) throws ClassNotFoundException;

	/**
	 * Determines if a field of a storage class, having the given field, is lazily loaded.
	 * 
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	boolean isLazilyLoaded(Class<?> type);

	/**
	 * Determines if a field of a storage class, having the given field, is eagerly loaded.
	 * 
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	boolean isEagerlyLoaded(Class<?> type);

	/**
	 * Yields the class token of the contract class.
	 * 
	 * @return the class token
	 */
	Class<?> getContract();

	/**
	 * Yields the class token of the storage class.
	 * 
	 * @return the class token
	 */
	Class<?> getStorage();

	/**
	 * Yields the class token of the account interface.
	 * 
	 * @return the class token
	 */
	Class<?> getAccount();

	/**
	 * Yields the class token of the interface for accounts
	 * that use the ed25519 algorithm for signing transactions.
	 * 
	 * @return the class token
	 */
	Class<?> getAccountED25519();

	/**
	 * Yields the class token of the interface for accounts
	 * that use the qtesla-p-I algorithm for signing transactions.
	 * 
	 * @return the class token
	 */
	Class<?> getAccountQTESLA1();

	/**
	 * Yields the class token of the interface for accounts
	 * that use the qtesla-p-III algorithm for signing transactions.
	 * 
	 * @return the class token
	 */
	Class<?> getAccountQTESLA3();

	/**
	 * Yields the class token of the interface for accounts
	 * that use the sha256dsa algorithm for signing transactions.
	 * 
	 * @return the class token
	 */
	Class<?> getAccountSHA256DSA();

	/**
	 * Yields the class token of the externally owned account class.
	 * 
	 * @return the class token
	 */
	Class<?> getExternallyOwnedAccount();

	/**
	 * Yields the class token of the abstract validators class.
	 * 
	 * @return the class token
	 */
	Class<?> getAbstractValidators();

	/**
	 * Yields the class token of gamete class.
	 * 
	 * @return the class token
	 */
	Class<?> getGamete();
}