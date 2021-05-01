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

package io.hotmoka.verification.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.constants.Constants;
import io.hotmoka.verification.IncompleteClasspathError;
import io.hotmoka.verification.TakamakaClassLoader;
import io.hotmoka.verification.ThrowIncompleteClasspathError;
import io.hotmoka.whitelisting.ResolvingClassLoader;
import io.hotmoka.whitelisting.WhiteListingWizard;

/**
 * A class loader used to access the definition of the classes of a Takamaka program.
 */
public class TakamakaClassLoaderImpl implements TakamakaClassLoader {

	/**
	 * The decorated resolving class loader.
	 */
	private final ResolvingClassLoader parent;

	/**
	 * The class token of the contract class.
	 */
	public final Class<?> contract;

	/**
	 * The class token of the gamete class.
	 */
	public final Class<?> gamete;

	/**
	 * The class token of the externally owned account class.
	 */
	public final Class<?> externallyOwnedAccount;

	/**
	 * The class token of the account interface.
	 */
	public final Class<?> account;

	/**
	 * The class token of the interface of the accounts that sign transactions with the sha256dsa algorithm.
	 */
	public final Class<?> accountSHA256DSA;

	/**
	 * The class token of the interface of the accounts that sign transactions with the qtesla-p-I algorithm.
	 */
	public final Class<?> accountQTESLA1;

	/**
	 * The class token of the interface of the accounts that sign transactions with the qtesla-p-III algorithm.
	 */
	public final Class<?> accountQTESLA3;

	/**
	 * The class token of the interface of the accounts that sign transactions with the ed25519 algorithm.
	 */
	public final Class<?> accountED25519;

	/**
	 * The class token of the storage class.
	 */
	public final Class<?> storage;

	/**
	 * The class token of the consensus update event class.
	 */
	public final Class<?> consensusUpdateEvent;

	/**
	 * The class token of the gas price update event class.
	 */
	public final Class<?> gasPriceUpdateEvent;

	/**
	 * The class token of the validators update event class.
	 */
	public final Class<?> validatorsUpdateEvent;

	/**
	 * Builds a class loader for the given jars, given as arrays of bytes.
	 * 
	 * @param jars the jars
	 * @param verificationVersion the version of the verification module that must b e used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 */
	public TakamakaClassLoaderImpl(Stream<byte[]> jars, int verificationVersion) {
		this.parent = ResolvingClassLoader.of(jars, verificationVersion);

		try {
			this.contract = loadClass(Constants.CONTRACT_NAME);
			this.externallyOwnedAccount = loadClass(Constants.EOA_NAME);
			this.gamete = loadClass(Constants.GAMETE_NAME);
			this.account = loadClass(Constants.ACCOUNT_NAME);
			this.accountED25519 = loadClass(Constants.ACCOUNT_ED25519_NAME);
			this.accountQTESLA1 = loadClass(Constants.ACCOUNT_QTESLA1_NAME);
			this.accountQTESLA3 = loadClass(Constants.ACCOUNT_QTESLA3_NAME);
			this.accountSHA256DSA = loadClass(Constants.ACCOUNT_SHA256DSA_NAME);
			this.storage = loadClass(Constants.STORAGE_NAME);
			this.consensusUpdateEvent = loadClass(Constants.CONSENSUS_UPDATE_NAME);
			this.gasPriceUpdateEvent = loadClass(Constants.GAS_PRICE_UPDATE_NAME);
			this.validatorsUpdateEvent = loadClass(Constants.VALIDATORS_UPDATE_NAME);
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	@Override
	public final int getVerificationVersion() {
		return parent.getVerificationVersion();
	}

	@Override
	public final boolean isStorage(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> storage.isAssignableFrom(loadClass(className)));
	}

	@Override
	public final boolean isContract(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> contract.isAssignableFrom(loadClass(className)));
	}

	@Override
	public final boolean isConsensusUpdateEvent(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> consensusUpdateEvent.isAssignableFrom(loadClass(className)));
	}

	@Override
	public final boolean isGasPriceUpdateEvent(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> gasPriceUpdateEvent.isAssignableFrom(loadClass(className)));
	}

	@Override
	public final boolean isValidatorsUpdateEvent(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> validatorsUpdateEvent.isAssignableFrom(loadClass(className)));
	}

	@Override
	public final boolean isa(String className, String superclassName) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> loadClass(superclassName).isAssignableFrom(loadClass(className)));
	}

	@Override
	public final boolean isExported(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> Stream.of(loadClass(className).getAnnotations()).anyMatch(annotation -> Constants.EXPORTED_NAME.equals(annotation.annotationType().getName())));
	}

	@Override
	public final boolean isInterface(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> loadClass(className).isInterface());
	}

	@Override
	public final boolean isLazilyLoaded(Class<?> type) {
		return !type.isPrimitive() && type != String.class && type != BigInteger.class && !type.isEnum();
	}

	@Override
	public final boolean isEagerlyLoaded(Class<?> type) {
		return !isLazilyLoaded(type);
	}

	@Override
	public final Class<?> getContract() {
		return contract;
	}

	@Override
	public final Class<?> getStorage() {
		return storage;
	}

	@Override
	public final Class<?> getExternallyOwnedAccount() {
		return externallyOwnedAccount;
	}

	@Override
	public final Class<?> getGamete() {
		return gamete;
	}

	@Override
	public final Class<?> getAccount() {
		return account;
	}

	@Override
	public final Class<?> getAccountED25519() {
		return accountED25519;
	}

	@Override
	public final Class<?> getAccountQTESLA1() {
		return accountQTESLA1;
	}

	@Override
	public Class<?> getAccountQTESLA3() {
		return accountQTESLA3;
	}

	@Override
	public final Class<?> getAccountSHA256DSA() {
		return accountSHA256DSA;
	}

	@Override
	public final WhiteListingWizard getWhiteListingWizard() {
		return parent.getWhiteListingWizard();
	}

	@Override
	public final Class<?> loadClass(String className) throws ClassNotFoundException {
		return parent.loadClass(className);
	}

	@Override
	public final Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException {
		return parent.resolveField(className, name, type);
	}

	@Override
	public final Optional<Field> resolveField(Class<?> clazz, String name, Class<?> type) {
		return parent.resolveField(clazz, name, type);
	}

	@Override
	public final Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException {
		return parent.resolveConstructor(className, args);
	}

	@Override
	public final Optional<Constructor<?>> resolveConstructor(Class<?> clazz, Class<?>[] args) {
		return parent.resolveConstructor(clazz, args);
	}

	@Override
	public final Optional<java.lang.reflect.Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveMethod(className, methodName, args, returnType);
	}

	@Override
	public final Optional<java.lang.reflect.Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveMethod(clazz, methodName, args, returnType);
	}

	@Override
	public final Optional<Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveInterfaceMethod(className, methodName, args, returnType);
	}

	@Override
	public final Optional<Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveInterfaceMethod(clazz, methodName, args, returnType);
	}

	@Override
	public ClassLoader getJavaClassLoader() {
		return parent.getJavaClassLoader();
	}
}