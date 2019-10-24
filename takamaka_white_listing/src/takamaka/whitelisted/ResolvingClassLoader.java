package takamaka.whitelisted;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A class loader that implements resolution methods for fields, constructors and methods,
 * according to Java's resolution rules.
 */
public abstract class ResolvingClassLoader extends URLClassLoader implements AutoCloseable {

	/**
	 * An object that knows about methods that can be called from Takamaka code
	 * and under which conditions.
	 */
	public final WhiteListingWizard whiteListingWizard = new WhiteListingWizard(this);

	/**
	 * Builds a class loader with the given URLs.
	 * 
	 * @param urls the urls that make up the class path
	 */
	protected ResolvingClassLoader(URL[] urls) {
		super(urls, ClassLoader.getSystemClassLoader());
	}

	/**
	 * Yields the field resolved from the given static description.
	 * 
	 * @param className the name of the class from the field look-up must start
	 * @param name the name of the field
	 * @param type the type of the field
	 * @return the resolved field, if any
	 * @throws ClassNotFoundException if some class could not be found during resolution
	 */
	public final Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException {
		for (Class<?> clazz = loadClass(className); clazz != null; clazz = clazz.getSuperclass()) {
			Optional<Field> result = Stream.of(clazz.getDeclaredFields())
					.filter(field -> field.getType() == type && field.getName().equals(name))
					.findFirst();

			if (result.isPresent())
				return result;
		}

		return Optional.empty();
	}

	/**
	 * Yields the constructor resolved from the given static description.
	 * 
	 * @param className the name of the class declaring the constructor
	 * @param args the arguments of the constructor
	 * @return the resolved constructor, if any
	 * @throws ClassNotFoundException if some class could not be found during resolution
	 */
	public final Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException {
		try {
			return Optional.of(loadClass(className).getDeclaredConstructor(args));
		}
		catch (NoSuchMethodException e) {
			return Optional.empty();
		}
	}

	/**
	 * Yields the method resolved from the given static description.
	 * 
	 * @param className the name of the class from which the method look-up must start
	 * @param methodName the name of the method
	 * @param args the arguments of the method
	 * @param returnType the return type of the method
	 * @return the resolved method, if any. It is defined in {@code className} or in one of its superclasses or implemented interfaces
	 * @throws ClassNotFoundException if some class could not be found during resolution
	 */
	public final Optional<java.lang.reflect.Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		Class<?> clazz = loadClass(className);

		for (Class<?> cursor = clazz; cursor != null; cursor = cursor.getSuperclass()) {
			Optional<java.lang.reflect.Method> result = resolveMethodExact(cursor, methodName, args, returnType);
			if (result.isPresent())
				return result;
		}

		return resolveMethodInInterfacesOf(clazz, methodName, args, returnType);
	}

	/**
	 * Yields the interface method resolved from the given static description.
	 * 
	 * @param className the name of the class from which the method look-up must start
	 * @param methodName the name of the method
	 * @param args the arguments of the method
	 * @param returnType the return type of the method
	 * @return the resolved method, if any. It is defined in {@code className} or in one of its implemented interfaces
	 * @throws ClassNotFoundException if some class could not be found during resolution
	 */
	public final Optional<java.lang.reflect.Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return resolveInterfaceMethod(loadClass(className), methodName, args, returnType);
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
	 * Yields the interface method resolved from the given static description.
	 * 
	 * @param clazz the class from which the method look-up must start
	 * @param methodName the name of the method
	 * @param args the arguments of the method
	 * @param returnType the return type of the method
	 * @return the resolved method, if any. It is defined in {@code className} or in one of its implemented interfaces
	 * @throws ClassNotFoundException if some class could not be found during resolution
	 */
	private static Optional<java.lang.reflect.Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		Optional<java.lang.reflect.Method> result = resolveMethodExact(clazz, methodName, args, returnType);
		return result.isPresent() ? result : resolveMethodInInterfacesOf(clazz, methodName, args, returnType);
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
	private static Optional<java.lang.reflect.Method> resolveMethodInInterfacesOf(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
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