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
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.hotmoka.constants.Constants;
import io.hotmoka.whitelisting.ResolvingClassLoader;
import io.hotmoka.whitelisting.WhiteListingWizard;

/**
 * A sealed implementation of a {@link io.hotmoka.whitelisting.ResolvingClassLoader}.
 */
public class ResolvingClassLoaderImpl extends ClassLoader implements ResolvingClassLoader {

	/**
	 * The version of the verification module that must b e used; this affects the
	 * set of white-listing annotations used by the class loader.
	 */
	private final int verificationVersion;

	/**
	 * An object that knows about methods that can be called from Takamaka code and under which conditions.
	 */
	private final WhiteListingWizard whiteListingWizard;

	/**
	 * The jars of the classpath of this class loader.
	 */
	private final byte[][] jars;

	private final static String TAKAMAKA_PACKAGE_NAME_WITH_SLASHES = Constants.IO_TAKAMAKA_CODE_PACKAGE_NAME.replace('.', '/');

	private final static String WHITELISTING_PACKAGE_NAME = ResolvingClassLoader.class.getPackageName() + '.';

	/**
	 * Builds a class loader with the given jars.
	 * 
	 * @param jars the jars, as arrays of bytes
	 * @param verificationVersion the version of the verification module that must b e used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 */
	public ResolvingClassLoaderImpl(Stream<byte[]> jars, int verificationVersion) {
		super(null);

		this.verificationVersion = verificationVersion;
		this.jars = jars.toArray(byte[][]::new);
		this.whiteListingWizard = new WhiteListingWizardImpl(this);
	}

	@Override
	public final int getVerificationVersion() {
		return verificationVersion;
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> clazz = loadClassFromCache(name)
			.or(() -> loadClassFromBootstrapClassloader(name))
			.or(() -> loadClassFromApplicationClassloader(name))
			.or(() -> loadClassFromJarsInNode(name))
			.orElseThrow(() -> new ClassNotFoundException(name));

        if (resolve)
			resolveClass(clazz);

        return clazz;
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
				|| Constants.DUMMY_NAME.equals(name) // to allow instrumented methods
				|| Constants.RUNTIME_NAME.equals(name)) // to allow calls to Takamaka's runtime
			try {
				return Optional.of(ClassLoader.getSystemClassLoader().loadClass(name));
			}
			catch (ClassNotFoundException e) {
				// ignore it
			}

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
		try (InputStream in = getResourceAsStream(name.replace('.', '/') + ".class")) {
			if (in != null) {
				byte[] bytes = in.readAllBytes();
				Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
				return Optional.of(clazz);
			}
		}
		catch (IOException | ClassFormatError e) {
			throw new RuntimeException(e);
		}

		return Optional.empty();
	}

    @Override
    public InputStream getResourceAsStream(String name) {
    	return getResourceAsStreamFromBoostrapClassloader(name)
    		.or(() -> getResourceAsStreamFromApplicationClassloader(name))
    		.or(() -> getResourceAsStreamFromJarsInNode(name))
    		.orElse(null);
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
    		catch (IOException e) {
    			throw new UncheckedIOException(e);
    		}
            finally {
                // only close the stream if the entry could not be found
                if (jis != null && !found)
                    try {
                        jis.close();
                    }
                    catch (IOException e) {
                        // ignore me
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
    	// since they are used by the instrumentation code or for checking white-listing annotations;
    	// for them, we use the application (aka system) class-loader, that takes into account
    	// the full classpath of the JVM running the node
    	if (name.startsWith(TAKAMAKA_PACKAGE_NAME_WITH_SLASHES))
    		return Optional.ofNullable(ClassLoader.getSystemClassLoader().getResourceAsStream(name));
    	else
    		return Optional.empty();
	}

    @Override
	public ClassLoader getJavaClassLoader() {
		return this;
	}

	@Override
	public WhiteListingWizard getWhiteListingWizard() {
		return whiteListingWizard;
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