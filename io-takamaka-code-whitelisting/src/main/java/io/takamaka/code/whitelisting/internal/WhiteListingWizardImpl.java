package io.takamaka.code.whitelisting.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import io.takamaka.code.whitelisting.MissingWhiteListingAnnotationsError;
import io.takamaka.code.whitelisting.WhiteListingWizard;

/**
 * A sealed implementation of a white-listing wizard.
 */
class WhiteListingWizardImpl implements WhiteListingWizard {
	private final static String WHITE_LISTED_ROOT = WhiteListingWizardImpl.class.getPackage().getName() + ".database";

	/**
	 * The class loader used to load the classes whose code is checked for white-listing.
	 */
	private final ResolvingClassLoaderImpl classLoader;

	/**
	 * The package name where the white-listing annotations are looked for.
	 */
	private final String whiteListedRootWithVersion;

	/**
	 * Builds a wizard.
	 * 
	 * @param classLoader the class loader used to load the classes whose code is checked for white-listing
	 */
	WhiteListingWizardImpl(ResolvingClassLoaderImpl classLoader) {
		this.classLoader = classLoader;
		this.whiteListedRootWithVersion = WHITE_LISTED_ROOT + ".version" + classLoader.getVerificationVersion() + ".";
		ensureVerificationVersionExistsInDatabase();
	}

	/**
	 * Tries to load the white-listing annotations of class java.lang.Object from the database.
	 * 
	 * @throws MissingWhiteListingAnnotationsError if the annotations cannot be found in the database
	 */
	private void ensureVerificationVersionExistsInDatabase() {
		try {
			classLoader.loadClass(whiteListedRootWithVersion + Object.class.getName());
		}
		catch (ClassNotFoundException e) {
			throw new MissingWhiteListingAnnotationsError(classLoader.getVerificationVersion());
		}
	}

	@Override
	public Optional<Field> whiteListingModelOf(Field field) {
		// if the class defining the field has been loaded by the blockchain class loader,
		// then it comes from blockchain and the field is white-listed
		if (field.getDeclaringClass().getClassLoader() == classLoader)
			return Optional.of(field);
		else
			return fieldInWhiteListedLibraryFor(field);
	}

	@Override
	public Optional<Constructor<?>> whiteListingModelOf(Constructor<?> constructor) {
		// if the class defining the constructor has been loaded by the blockchain class loader,
		// then it comes from blockchain and the constructor is white-listed
		Class<?> declaringClass = constructor.getDeclaringClass();

		if (declaringClass.getClassLoader() == classLoader)
			return Optional.of(constructor);
		else
			return constructorInWhiteListedLibraryFor(constructor);
	}

	@Override
	public Optional<Method> whiteListingModelOf(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();

		if (declaringClass.getClassLoader() == classLoader)
			// if the class defining the method has been loaded by the blockchain class loader,
			// then it comes from blockchain and the method is white-listed
			return Optional.of(method);
		else {
			// otherwise we check in the possibly overridden methods
			Optional<Method> result = methodInWhiteListedLibraryFor(method);
			if (result.isPresent())
				return result;

			// a method might not be explicitly white-listed, but it might override a method
			// of a superclass that is white-listed. Hence we check that possibility
			if (!Modifier.isStatic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())) {
				Class<?> superclass = declaringClass.getSuperclass();
				// all interfaces extend Object
				if (superclass == null && declaringClass.isInterface())
					superclass = Object.class;

				if (superclass != null) {
					Optional<Method> overridden = classLoader.resolveMethod(superclass, method.getName(), method.getParameterTypes(), method.getReturnType());
					if (overridden.isPresent()) {
						result = whiteListingModelOf(overridden.get());
						if (result.isPresent())
							return result;
					}
				}

				for (Class<?> superinterface: declaringClass.getInterfaces()) {
					Optional<Method> overridden = classLoader.resolveMethod(superinterface, method.getName(), method.getParameterTypes(), method.getReturnType());
					if (overridden.isPresent()) {
						result = whiteListingModelOf(overridden.get());
						if (result.isPresent())
							return result;
					}
				}
			}
		}

		return Optional.empty();
	}

	private Optional<Field> fieldInWhiteListedLibraryFor(Field field) {
		try {
			return classLoader.resolveField(mirrorClassNameFor(field), field.getName(), field.getType());
		}
		catch (ClassNotFoundException e) {
			// the field is not in the library of white-listed code
			return Optional.empty();
		}
	}

	private Optional<Constructor<?>> constructorInWhiteListedLibraryFor(Constructor<?> constructor) {
		try {
			return classLoader.resolveConstructor(mirrorClassNameFor(constructor), constructor.getParameterTypes());
		}
		catch (ClassNotFoundException e) {
			// the constructor is not in the library of white-listed code
			return Optional.empty();
		}
	}

	private Optional<java.lang.reflect.Method> methodInWhiteListedLibraryFor(java.lang.reflect.Method method) {
		// Method Object.getClass() is white-listed but we cannot put it in the white-listed library, since that method is final in Object
		if (method.getDeclaringClass() == Object.class && "getClass".equals(method.getName()))
			try {
				return Optional.of(Object.class.getMethod("getClass"));
			}
			catch (NoSuchMethodException | SecurityException e) {
				// this will never happen
				throw new IllegalStateException("Cannot access method Object.getClass()");
			}
	
		try {
			return classLoader.resolveMethodExact(mirrorClassNameFor(method), method.getName(), method.getParameterTypes(), method.getReturnType());
		}
		catch (ClassNotFoundException e) {
			// the method is not in the library of white-listed code
			return Optional.empty();
		}
	}

	/**
	 * Yields the name of the mirror of the class of the given member, in the white-listing database.

	 * @param member the member
	 * @return the name of the mirror class
	 */
	private String mirrorClassNameFor(Member member) {
		return whiteListedRootWithVersion + member.getDeclaringClass().getName();
	}
}