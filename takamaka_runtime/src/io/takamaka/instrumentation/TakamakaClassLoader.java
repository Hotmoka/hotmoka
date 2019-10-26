package io.takamaka.instrumentation;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.instrumentation.internal.ThrowIncompleteClasspathError;
import io.takamaka.whitelisting.ResolvingClassLoader;
import io.takamaka.whitelisting.WhiteListingWizard;

/**
 * A class loader used to access the definition of the classes of a Takamaka program.
 */
public class TakamakaClassLoader implements ResolvingClassLoader {

	/**
	 * The decorated resolving class loader.
	 */
	private final ResolvingClassLoader parent;

	/**
	 * The class token of the contract class.
	 */
	public final Class<?> contractClass;

	/**
	 * The class token of the externally owned account class.
	 */
	public final Class<?> externallyOwnedAccount;

	/**
	 * The class token of the storage class.
	 */
	public final Class<?> storageClass;

	/**
	 * Builds a class loader with the given URLs.
	 */
	public TakamakaClassLoader(URL[] urls) {
		this.parent = ResolvingClassLoader.of(urls);

		try {
			this.contractClass = loadClass("io.takamaka.lang.Contract");
			this.externallyOwnedAccount = loadClass("io.takamaka.lang.ExternallyOwnedAccount");
			this.storageClass = loadClass("io.takamaka.lang.Storage");
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	/**
	 * Determines if a class is a storage class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that class extends {@link takamaka.blockchain.runtime.AbstractStorage}
	 */
	public final boolean isStorage(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> storageClass.isAssignableFrom(loadClass(className)));
	}

	/**
	 * Checks if a class is a contract.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	public final boolean isContract(String className) {
		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> contractClass.isAssignableFrom(loadClass(className)));
	}

	/**
	 * Determines if a field of a storage class, having the given field, is lazily loaded.
	 * 
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	public final boolean isLazilyLoaded(Class<?> type) {
		return !type.isPrimitive() && type != String.class && type != BigInteger.class && !type.isEnum();
	}

	/**
	 * Determines if a field of a storage class, having the given field, is eagerly loaded.
	 * 
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	public final boolean isEagerlyLoaded(Class<?> type) {
		return !isLazilyLoaded(type);
	}

	/**
	 * Computes the Java token class for the given BCEL type.
	 * 
	 * @param type the BCEL type
	 * @return the class token corresponding to {@code type}
	 */
	public final Class<?> bcelToClass(Type type) {
		if (type == BasicType.BOOLEAN)
			return boolean.class;
		else if (type == BasicType.BYTE)
			return byte.class;
		else if (type == BasicType.CHAR)
			return char.class;
		else if (type == BasicType.DOUBLE)
			return double.class;
		else if (type == BasicType.FLOAT)
			return float.class;
		else if (type == BasicType.INT)
			return int.class;
		else if (type == BasicType.LONG)
			return long.class;
		else if (type == BasicType.SHORT)
			return short.class;
		else if (type == BasicType.VOID)
			return void.class;
		else if (type instanceof ObjectType)
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> loadClass(type.toString()));
		else { // array
			Class<?> elementsClass = bcelToClass(((ArrayType) type).getElementType());
			// trick: we build an array of 0 elements just to access its class token
			return java.lang.reflect.Array.newInstance(elementsClass, 0).getClass();
		}
	}

	/**
	 * Computes the Java token classes for the given BCEL types.
	 * 
	 * @param types the BCEL types
	 * @return the class tokens corresponding to {@code types}
	 */
	public final Class<?>[] bcelToClass(Type[] types) {
		Class<?>[] result = new Class<?>[types.length];
		for (int pos = 0; pos < result.length; pos++)
			result[pos] = bcelToClass(types[pos]);
	
		return result;
	}

	@Override
	public Stream<URL> getOrigins() {
		return parent.getOrigins();
	}

	@Override
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