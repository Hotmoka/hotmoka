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

package io.hotmoka.whitelisting.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.hotmoka.whitelisting.WhiteListingClassLoaders;
import io.hotmoka.whitelisting.WhitelistingConstants;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import io.hotmoka.whitelisting.api.WhiteListingClassLoader;
import io.hotmoka.whitelisting.api.WhiteListingWizard;

/**
 * An implementation of a {@link io.hotmoka.whitelisting.WhiteListingClassLoader}.
 */
public class WhiteListingClassLoaderImpl extends ClassLoader implements WhiteListingClassLoader {

	/**
	 * The version of the verification module that must be used; this affects the
	 * set of white-listing annotations used by the class loader.
	 */
	private final long verificationVersion;

	/**
	 * An object that knows about methods that can be called from Takamaka code and under which conditions.
	 */
	private final WhiteListingWizard whiteListingWizard;

	/**
	 * The jars of the classpath of this class loader.
	 */
	private final byte[][] jars;

	/**
	 * The system class loader.
	 */
	private final ClassLoader systemClassLoader;

	private final static String WHITELISTING_PACKAGE_NAME = WhiteListingClassLoaders.class.getPackageName() + '.';

	private final static String DUMMY_NAME_WITH_SLASHES = WhitelistingConstants.DUMMY_NAME.replace('.', '/') + ".class";

	private final static Logger LOGGER = Logger.getLogger(WhiteListingClassLoaderImpl.class.getName());

	/**
	 * Builds a class loader for the classes in the given jars.
	 * 
	 * @param jars the jars, as arrays of bytes
	 * @param verificationVersion the version of the verification module that must be used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 * @throws UnsupportedVerificationVersionException if the annotations for the required verification
	 *                                                 version cannot be found in the database
	 */
	public WhiteListingClassLoaderImpl(Stream<byte[]> jars, long verificationVersion) throws UnsupportedVerificationVersionException {
		super(null);

		this.verificationVersion = verificationVersion;
		this.systemClassLoader = ClassLoader.getSystemClassLoader();
		this.jars = jars.toArray(byte[][]::new);
		this.whiteListingWizard = new WhiteListingWizardImpl(this);
	}

	@Override
	public final long getVerificationVersion() {
		return verificationVersion;
	}

	@Override
    public InputStream getResourceAsStream(String name) {
    	return getResourceAsStreamFromBoostrapClassloader(name)
    		.or(() -> getResourceAsStreamFromApplicationClassloader(name))
    		.or(() -> getResourceAsStreamFromJarsInNode(name))
    		.orElse(null);
    }

    @Override
	public ClassLoader getJavaClassLoader() {
		return this;
	}

	@Override
	public final WhiteListingWizard getWhiteListingWizard() {
		return whiteListingWizard;
	}

	@Override
	public final Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException {
		return resolveField(loadClass(className), name, type);
	}

	@Override
	public final Optional<Field> resolveField(Class<?> clazz, String name, Class<?> type) throws ClassNotFoundException {
		for (Class<?> cursor = clazz; cursor != null; cursor = cursor.getSuperclass()) {
			Optional<Field> result;

			try {
				result = Stream.of(cursor.getDeclaredFields())
						.filter(field -> field.getType() == type && field.getName().equals(name))
						.findFirst();
			}
			catch (NoClassDefFoundError e) {
				throw new ClassNotFoundException(e.getMessage());
			}

			if (result.isPresent())
				return result;
		}

		return Optional.empty();
	}

	@Override
	public final Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException {
		return resolveConstructor(loadClass(className), args);
	}

	@Override
	public final Optional<Constructor<?>> resolveConstructor(Class<?> clazz, Class<?>[] args) throws ClassNotFoundException {
		try {
			return Optional.of(clazz.getDeclaredConstructor(args));
		}
		catch (NoSuchMethodException e) {
			return Optional.empty();
		}
		catch (NoClassDefFoundError e) {
			throw new ClassNotFoundException(e.getMessage());
		}
	}

	@Override
	public final Optional<Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return resolveMethod(loadClass(className), methodName, args, returnType);
	}

	@Override
	public final Optional<Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		for (Class<?> cursor = clazz; cursor != null; cursor = cursor.getSuperclass()) {
			Optional<Method> result = resolveMethodExact(cursor, methodName, args, returnType);
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
	public final Optional<Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		// we first try to resolve the method in Object, since all implementations of the interface must extend Object
		var maybeMethod = resolveInterfaceMethodInObject(methodName, args, returnType);
		if (maybeMethod.isPresent())
			return maybeMethod;

		maybeMethod = resolveMethodExact(clazz, methodName, args, returnType);
		if (maybeMethod.isPresent())
			return maybeMethod;

		return resolveMethodInInterfacesOf(clazz, methodName, args, returnType);
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
	 * @throws ClassNotFoundException if some class could not be resolved
	 */
	Optional<Method> resolveMethodExact(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return resolveMethodExact(loadClass(className), methodName, args, returnType);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			var clazz = loadClassFromCache(name)
				.or(() -> loadClassFromBootstrapClassloader(name))
				.or(() -> loadClassFromApplicationClassloader(name))
				.or(() -> loadClassFromJarsInNode(name))
				.orElseThrow(() -> new ClassNotFoundException(name));
	
			if (resolve)
				resolveClass(clazz);
	
			return clazz;
		}
	}

	private Optional<Class<?>> loadClassFromCache(String name) {
		return Optional.ofNullable(findLoadedClass(name));
	}

	private Optional<Class<?>> loadClassFromApplicationClassloader(String name) {
		// there are some classes that need to be loaded from the running classpath of the node itself,
		// since they are used by the instrumentation code or for checking white-listing annotations;
		// for them, we use the application (aka system) class-loader, that takes into account
		// the full classpath of the JVM running the node
		if (name.startsWith(WHITELISTING_PACKAGE_NAME) // to allow access to the white-listing database
				|| WhitelistingConstants.DUMMY_NAME.equals(name) // to allow instrumented methods
				|| WhitelistingConstants.RUNTIME_NAME.equals(name)) { // to allow calls to Takamaka's runtime
			try {
				return Optional.of(systemClassLoader.loadClass(name));
			}
			catch (ClassNotFoundException e) {
				return Optional.empty();
			}
		}
		else
			return Optional.empty();
	}

	private Optional<Class<?>> loadClassFromBootstrapClassloader(String name) {
		try {
			return Optional.of(super.loadClass(name, false));
		}
		catch (ClassNotFoundException e) {
			return Optional.empty();
		}
	}

	private Optional<Class<?>> loadClassFromJarsInNode(String name) {
		try (var in = getResourceAsStream(name.replace('.', '/') + ".class")) {
			if (in != null) {
				byte[] bytes = in.readAllBytes();
				return Optional.of(defineClass(name, bytes, 0, bytes.length));
			}
		}
		catch (IOException | ClassFormatError e) {
			// the jars are malformed, but it's not our fault
			return Optional.empty();
		}
	
		return Optional.empty();
	}

	private Optional<Method> resolveInterfaceMethodInObject(String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return resolveMethodExact(loadClass(Object.class.getName()), methodName, args, returnType);
	}

	private Optional<InputStream> getResourceAsStreamFromJarsInNode(String name) {
		boolean found = false;
		for (byte[] jar: jars) {
	        ZipInputStream jis = null;
	
	        try {
	        	jis = new ZipInputStream(new ByteArrayInputStream(jar));
				ZipEntry entry;
				while ((entry = jis.getNextEntry()) != null)
					if (entry.getName().equals(name)) {
						found = true;
						return Optional.of(jis);
					}
	        }
			catch (IOException e) { // includes ZipException
				LOGGER.log(Level.WARNING, "could not access the jar file", e);
				return Optional.empty();
			}
	        finally {
	            // only close the stream if the entry could not be found
	            if (jis != null && !found)
	                try {
	                    jis.close();
	                }
	                catch (IOException e) {
	                	LOGGER.log(Level.WARNING, "could not close the stream", e);
	                }
	        }
		}
	
		return Optional.empty();
	}

	private Optional<InputStream> getResourceAsStreamFromBoostrapClassloader(String name) {
		return Optional.ofNullable(super.getResourceAsStream(name));
	}

	private Optional<InputStream> getResourceAsStreamFromApplicationClassloader(String name) {
		// there are some classes that need to be loaded from the node itself,
		// since they are used by the instrumentation code;
		// for them, we use the application (aka system) class-loader, that takes into account
		// the full classpath of the JVM running the node
		if (DUMMY_NAME_WITH_SLASHES.equals(name)) // to allow instrumented methods
			return Optional.ofNullable(systemClassLoader.getResourceAsStream(name));
		else
			return Optional.empty();
	}

	/**
	 * Yields the method of an interface implemented by the given class with the given signature. It does not look in superclasses.
	 * 
	 * @param clazz the class
	 * @param methodName the name of the method
	 * @param args the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return the method, if any
	 * @throws ClassNotFoundException if some class cannot be resolved
	 */
	private Optional<Method> resolveMethodInInterfacesOf(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		try {
			for (Class<?> interf: clazz.getInterfaces()) {
				Optional<Method> result = resolveInterfaceMethod(interf, methodName, args, returnType);
				if (result.isPresent())
					return result;
			}
		}
		catch (NoClassDefFoundError e) {
			throw new ClassNotFoundException(e.getMessage());
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
	 * @throws ClassNotFoundException if some class cannot be resolved
	 */
	private static Optional<Method> resolveMethodExact(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		try {
			return Stream.of(clazz.getDeclaredMethods())
					.filter(method -> method.getReturnType() == returnType && method.getName().equals(methodName) && Arrays.equals(method.getParameterTypes(), args))
					.findFirst();
		}
		catch (NoClassDefFoundError e) {
			throw new ClassNotFoundException(e.getMessage());
		}
	}
}