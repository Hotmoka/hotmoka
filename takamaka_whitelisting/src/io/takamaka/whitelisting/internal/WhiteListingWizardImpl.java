package io.takamaka.whitelisting.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import io.takamaka.whitelisting.WhiteListed;
import io.takamaka.whitelisting.WhiteListingWizard;

/**
 * A sealed implementation of a white-listing wizard.
 */
class WhiteListingWizardImpl implements WhiteListingWizard {
	private final static String WHITE_LISTED_ROOT = WhiteListingWizard.class.getPackage().getName() + ".database";

	/**
	 * The class loader used to load the classes whose code is checked for white-listing.
	 */
	private final ResolvingClassLoaderImpl classLoader;

	/**
	 * Builds a wizard.
	 * 
	 * @param classLoader the class loader used to load the classes whose code is checked for white-listing
	 */
	WhiteListingWizardImpl(ResolvingClassLoaderImpl classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public Optional<Field> whiteListingModelOf(Field field) {
		// if the class defining the field has been loaded by the blockchain class loader,
		// then it comes from blockchain and the field is white-listed
		if (field.getDeclaringClass().getClassLoader() == classLoader)
			return Optional.of(field);
		// otherwise, since fields cannot be redefined in Java, either is the field explicitly
		// annotated as white-listed, or it is not white-listed
		else if (field.isAnnotationPresent(WhiteListed.class))
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
		// otherwise, since constructors cannot be redefined in Java, either is the constructor explicitly
		// annotated as white-listed, or it is not white-listed
		else if (constructor.isAnnotationPresent(WhiteListed.class))
			return Optional.of(constructor);
		else
			return constructorInWhiteListedLibraryFor((Constructor<?>) constructor);
	}

	@Override
	public Optional<Method> whiteListingModelOf(Method method) {
		// if the class defining the method has been loaded by the blockchain class loader,
		// then it comes from blockchain and the method is white-listed
		Class<?> declaringClass = method.getDeclaringClass();

		if (declaringClass.getClassLoader() == classLoader)
			return Optional.of(method);
		// otherwise, either the method is explicitly annotated as white-listed
		else if (method.isAnnotationPresent(WhiteListed.class))
			return Optional.of(method);
		else {
			// or we check in the possibly overridden methods
			Optional<Method> result = methodInWhiteListedLibraryFor(method);
			if (result.isPresent())
				return result;

			// a method might not be explicitly white-listed, but it might override a method
			// of a superclass that is white-listed. Hence we check that possibility
			if (!Modifier.isStatic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())) {
				Class<?> superclass = declaringClass.getSuperclass();
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
	private static String mirrorClassNameFor(Member member) {
		return WHITE_LISTED_ROOT + "." + member.getDeclaringClass().getName();
	}
}