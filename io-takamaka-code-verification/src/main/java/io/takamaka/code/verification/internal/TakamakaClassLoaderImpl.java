package io.takamaka.code.verification.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.takamaka.code.constants.Constants;
import io.takamaka.code.verification.IncompleteClasspathError;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.whitelisting.ResolvingClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;

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
	 * The class token of the red/green contract class.
	 */
	public final Class<?> redGreenContract;

	/**
	 * The class token of the externally owned account class.
	 */
	public final Class<?> externallyOwnedAccount;

	/**
	 * The class token of the red/green externally owned account class.
	 */
	public final Class<?> redGreenExternallyOwnedAccount;

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
	 * Builds a class loader for the given jars, given as arrays of bytes.
	 * 
	 * @param jars the jars
	 * @param classNameProcessor a processor called whenever a new class is loaded with this class loader;
	 *                           it can be used to take note that a class with a given name comes from the
	 *                           n-th jar in {@code jars}
	 */
	public TakamakaClassLoaderImpl(Stream<byte[]> jars, BiConsumer<String, Integer> classNameProcessor) {
		this.parent = ResolvingClassLoader.of(jars, classNameProcessor);

		try {
			this.contract = loadClass(Constants.CONTRACT_NAME);
			this.redGreenContract = loadClass(Constants.RGCONTRACT_NAME);
			this.externallyOwnedAccount = loadClass(Constants.EOA_NAME);
			this.redGreenExternallyOwnedAccount = loadClass(Constants.RGEOA_NAME);
			this.account = loadClass(Constants.ACCOUNT_NAME);
			this.accountED25519 = loadClass(Constants.ACCOUNT_ED25519_NAME);
			this.accountQTESLA1 = loadClass(Constants.ACCOUNT_QTESLA1_NAME);
			this.accountQTESLA3 = loadClass(Constants.ACCOUNT_QTESLA3_NAME);
			this.accountSHA256DSA = loadClass(Constants.ACCOUNT_SHA256DSA_NAME);
			this.storage = loadClass(Constants.STORAGE_NAME);
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
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
	public final boolean isRedGreenContract(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> redGreenContract.isAssignableFrom(loadClass(className)));
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
	public final Class<?> getRedGreenContract() {
		return redGreenContract;
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
	public final Class<?> getRedGreenExternallyOwnedAccount() {
		return redGreenExternallyOwnedAccount;
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