package io.takamaka.code.whitelisting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import io.takamaka.code.whitelisting.internal.ResolvingClassLoaderImpl;

/**
 * A class loader that implements resolution methods for fields, constructors and methods,
 * according to Java's resolution rules.
 */
public interface ResolvingClassLoader {

	/**
	 * Yields an implementation of this interface that loads classes from the given jars, provided as byte arrays.
	 * 
	 * @param jars the jars, as byte arrays
	 * @param verificationVersion the version of the verification module that must b e used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 * @return the class loader
	 */
	static ResolvingClassLoader of(Stream<byte[]> jars, int verificationVersion) {
		return new ResolvingClassLoaderImpl(jars, verificationVersion);
	}

	/**
	 * Yields the version of the verification module that this class loader is using.
	 * 
	 * @return the version of the verification module
	 */
	int getVerificationVersion();

	/**
	 * Yields the Java class loader used internally by this class loader.
	 * 
	 * @return the Java class loader
	 */
	ClassLoader getJavaClassLoader();

	/**
	 * Loads the class with the given name, by using this class loader.
	 * 
	 * @param className the name of the class
	 * @return the class
	 * @throws ClassNotFoundException if the class cannot be found with this class loader
	 */
	Class<?> loadClass(String className) throws ClassNotFoundException;

	/**
	 * Yields a white-listing wizard that uses this class loader to load classes.
	 * 
	 * @return the wizard
	 */
	WhiteListingWizard getWhiteListingWizard();

	/**
	 * Yields the field resolved from the given static description.
	 * 
	 * @param className the name of the class from the field look-up must start
	 * @param name the name of the field
	 * @param type the type of the field
	 * @return the resolved field, if any
	 * @throws ClassNotFoundException if some class could not be found during resolution
	 */
	Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException;

	/**
	 * Yields the field resolved from the given static description.
	 * 
	 * @param clazz the class from the field look-up must start
	 * @param name the name of the field
	 * @param type the type of the field
	 * @return the resolved field, if any
	 */
	Optional<Field> resolveField(Class<?> clazz, String name, Class<?> type);

	/**
	 * Yields the constructor resolved from the given static description.
	 * 
	 * @param className the name of the class declaring the constructor
	 * @param args the arguments of the constructor
	 * @return the resolved constructor, if any
	 * @throws ClassNotFoundException if some class could not be found during resolution
	 */
	Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException;

	/**
	 * Yields the constructor resolved from the given static description.
	 * 
	 * @param clazz the class declaring the constructor
	 * @param args the arguments of the constructor
	 * @return the resolved constructor, if any
	 */
	Optional<Constructor<?>> resolveConstructor(Class<?> clazz, Class<?>[] args);

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
	Optional<java.lang.reflect.Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException;

	/**
	 * Yields the method resolved from the given static description.
	 * 
	 * @param clazz the class from which the method look-up must start
	 * @param methodName the name of the method
	 * @param args the arguments of the method
	 * @param returnType the return type of the method
	 * @return the resolved method, if any. It is defined in {@code className} or in one of its superclasses or implemented interfaces
	 */
	Optional<java.lang.reflect.Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType);

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
	Optional<Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException;

	/**
	 * Yields the interface method resolved from the given static description.
	 * 
	 * @param clazz the class from which the method look-up must start
	 * @param methodName the name of the method
	 * @param args the arguments of the method
	 * @param returnType the return type of the method
	 * @return the resolved method, if any. It is defined in {@code className} or in one of its implemented interfaces
	 */
	Optional<Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType);
}