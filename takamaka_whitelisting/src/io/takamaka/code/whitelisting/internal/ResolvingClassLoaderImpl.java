package io.takamaka.code.whitelisting.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import io.takamaka.code.whitelisting.ResolvingClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;

/**
 * A sealed implementation of a {@link io.takamaka.code.whitelisting.ResolvingClassLoader}.
 */
public class ResolvingClassLoaderImpl extends URLClassLoader implements ResolvingClassLoader {

	/**
	 * An object that knows about methods that can be called from Takamaka code
	 * and under which conditions.
	 */
	private WhiteListingWizard whiteListingWizard = new WhiteListingWizardImpl(this);

	/**
	 * Builds a class loader with the given URLs.
	 * 
	 * @param urls the urls that make up the class path
	 */
	public ResolvingClassLoaderImpl(URL[] urls) {
		super(urls, ClassLoader.getSystemClassLoader());
	}

	@Override
	public WhiteListingWizard getWhiteListingWizard() {
		return whiteListingWizard;
	}

	@Override
	public Stream<URL> getOrigins() {
		return Stream.of(super.getURLs());
	}

	@Override
	public final Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException {
		return resolveField(loadClass(className), name, type);
	}

	@Override
	public Optional<Field> resolveField(Class<?> clazz, String name, Class<?> type) {
		while (clazz != null) {
			Optional<Field> result = Stream.of(clazz.getDeclaredFields())
				.filter(field -> field.getType() == type && field.getName().equals(name))
				.findFirst();

			if (result.isPresent())
				return result;
	
			clazz = clazz.getSuperclass();
		}

		return Optional.empty();
	}

	@Override
	public final Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException {
		return resolveConstructor(loadClass(className), args);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(Class<?> clazz, Class<?>[] args) {
		try {
			return Optional.of(clazz.getDeclaredConstructor(args));
		}
		catch (NoSuchMethodException e) {
			return Optional.empty();
		}
	}

	@Override
	public final Optional<java.lang.reflect.Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return resolveMethod(loadClass(className), methodName, args, returnType);
	}

	@Override
	public Optional<java.lang.reflect.Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		for (Class<?> cursor = clazz; cursor != null; cursor = cursor.getSuperclass()) {
			Optional<java.lang.reflect.Method> result = resolveMethodExact(cursor, methodName, args, returnType);
			if (result.isPresent())
				return result;
		}

		return resolveMethodInInterfacesOf(clazz, methodName, args, returnType);
	}

	@Override
	public final Optional<Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return resolveInterfaceMethod(loadClass(className), methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		Optional<java.lang.reflect.Method> result = resolveMethodExact(clazz, methodName, args, returnType);
		return result.isPresent() ? result : resolveMethodInInterfacesOf(clazz, methodName, args, returnType);
	}

	/**
	 * Yields the method of the given class with the given signature. It does not look
	 * in superclasses nor in super-interfaces.
	 * 
	 * @param className the name of the class
	 * @param methodName the name of the method
	 * @param args the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 * @throws ClassNotFoundException if some class could not be found during resolution
	 */
	Optional<Method> resolveMethodExact(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return resolveMethodExact(loadClass(className), methodName, args, returnType);
	}

	/**
	 * Yields the method of an interface implemented by the given class with the given signature. It does not look in superclasses.
	 * 
	 * @param clazz the class
	 * @param methodName the name of the method
	 * @param args the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 */
	private Optional<Method> resolveMethodInInterfacesOf(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		for (Class<?> interf: clazz.getInterfaces()) {
			Optional<java.lang.reflect.Method> result = resolveInterfaceMethod(interf, methodName, args, returnType);
			if (result.isPresent())
				return result;
		}
	
		return Optional.empty();
	}

	/**
	 * Yields the method of the given class with the given signature. It does not look
	 * in superclasses nor in super-interfaces.
	 * 
	 * @param clazz the class
	 * @param methodName the name of the method
	 * @param args the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 */
	private static Optional<Method> resolveMethodExact(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return Stream.of(clazz.getDeclaredMethods())
			.filter(method -> method.getReturnType() == returnType && method.getName().equals(methodName)
					&& Arrays.equals(method.getParameterTypes(), args))
			.findFirst();
	}
}