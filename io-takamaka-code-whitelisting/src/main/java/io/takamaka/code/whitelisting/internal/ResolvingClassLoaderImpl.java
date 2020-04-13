package io.takamaka.code.whitelisting.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.takamaka.code.whitelisting.ResolvingClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;

/**
 * A sealed implementation of a {@link io.takamaka.code.whitelisting.ResolvingClassLoader}.
 */
public class ResolvingClassLoaderImpl extends ClassLoader implements ResolvingClassLoader {

	/**
	 * An object that knows about methods that can be called from Takamaka code
	 * and under which conditions.
	 */
	private WhiteListingWizard whiteListingWizard = new WhiteListingWizardImpl(this);

	/**
	 * The jars of the classpath of this class loader.
	 */
	private final byte[][] jars;

	/**
	 * A processor called whenever a new class is loaded with this class loader.
	 */
	private final BiConsumer<String, Integer> classNameProcessor;

	/**
	 * Builds a class loader with the given jars.
	 * 
	 * @param jars the jars, as arrays of bytes
	 * @param classNameProcessor a processor called whenever a new class is loaded with this class loader;
	 *                           it can be used to take note that a class with a given name comes from the
	 *                           n-th jar in {@code jars}
	 */
	public ResolvingClassLoaderImpl(Stream<byte[]> jars, BiConsumer<String, Integer> classNameProcessor) {
		super(ClassLoader.getSystemClassLoader());

		this.jars = jars.toArray(byte[][]::new);
		this.classNameProcessor = classNameProcessor;
	}

	@Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if (clazz == null)
        	try {
        		clazz = super.loadClass(name, resolve);
        	}
        	catch (ClassNotFoundException cnfe) {
        		try {
        			InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
        			if (in != null)
        				try {
        					ByteArrayOutputStream out = new ByteArrayOutputStream();
        					in.transferTo(out);
        					byte[] bytes = out.toByteArray();
        					clazz = defineClass(name, bytes, 0, bytes.length);
        					if (resolve)
        						resolveClass(clazz);
        				}
	        			finally {
	        				try {
	        					in.close();
	        				}
	        				catch (IOException e) {
	        					// ignore me
	        				}
	        			}
        		}
        		catch (Exception e) {
        		}
        	}

        if (clazz == null)
        	throw new ClassNotFoundException(name);

        return clazz;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
    	InputStream result = super.getResourceAsStream(name);
    	if (result != null)
    		return result;

    	boolean found = false;
    	int pos = 0;
    	for (byte[] jar: jars) {
            ZipInputStream jis = null;

            try {
            	jis = new ZipInputStream(new ByteArrayInputStream(jar));
    			ZipEntry entry;
    			while ((entry = jis.getNextEntry()) != null)
    				if (entry.getName().equals(name)) {
    					found = true;
    					classNameProcessor.accept(name, pos);
    					return jis;
    				}

    			pos++;
            }
    		catch (IOException e) {
    			throw new UncheckedIOException(e);
    		}
            finally {
                // Only close the stream if the entry could not be found
                if (jis != null && !found)
                    try {
                        jis.close();
                    }
                    catch (IOException e) {
                        // ignore me
                    }
            }
    	}

    	return null;
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