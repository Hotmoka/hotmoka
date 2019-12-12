package io.takamaka.code.verification.internal;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

import io.takamaka.code.verification.Constants;
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
	 * The class token of the storage class.
	 */
	public final Class<?> storage;

	/**
	 * Builds a class loader with the given URLs.
	 */
	public TakamakaClassLoaderImpl(URL[] urls) {
		// we set the BCEL repository so that it matches the class path made up of the jar to
		// instrument and its dependencies. This is important since class instrumentation will use
		// the repository to infer least common supertypes during type inference, hence the
		// whole hierarchy of classes must be available to BCEL through its repository
		String appendedClassPath = Stream.of(urls).map(URL::getFile).collect(Collectors.joining(":"));
		Repository.setRepository(SyntheticRepository.getInstance(new ClassPath(appendedClassPath)));

		this.parent = ResolvingClassLoader.of(urls);

		try {
			this.contract = loadClass(Constants.CONTRACT_NAME);
			this.redGreenContract = loadClass(Constants.RGCONTRACT_NAME);
			this.externallyOwnedAccount = loadClass(Constants.EOA_NAME);
			this.redGreenExternallyOwnedAccount = loadClass(Constants.RGEOA_NAME);
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
	public Class<?> getRedGreenExternallyOwnedAccount() {
		return redGreenExternallyOwnedAccount;
	}

	@Override
	public final Stream<URL> getOrigins() {
		return parent.getOrigins();
	}

	public final WhiteListingWizard getWhiteListingWizard() {
		return parent.getWhiteListingWizard();
	}

	@Override
	public void close() throws IOException {
		parent.close();
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
}