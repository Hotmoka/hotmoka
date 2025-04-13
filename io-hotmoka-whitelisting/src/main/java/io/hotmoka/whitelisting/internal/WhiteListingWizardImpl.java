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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import io.hotmoka.whitelisting.api.WhiteListingWizard;

/**
 * An implementation of a white-listing wizard.
 */
class WhiteListingWizardImpl implements WhiteListingWizard {
	private final static String WHITE_LISTED_ROOT = WhiteListingWizardImpl.class.getPackage().getName() + ".database";

	/**
	 * The class loader used to load the classes whose code is checked for white-listing.
	 */
	private final WhiteListingClassLoaderImpl classLoader;

	/**
	 * The package name where the white-listing annotations are looked for.
	 */
	private final String whiteListedRootWithVersion;

	/**
	 * Builds a wizard.
	 * 
	 * @param classLoader the class loader used to load the classes whose code is checked for white-listing
	 * @throws UnsupportedVerificationVersionException if the annotations for the required verification
	 *                                                 version cannot be found in the database
	 */
	WhiteListingWizardImpl(WhiteListingClassLoaderImpl classLoader) throws UnsupportedVerificationVersionException {
		this.classLoader = classLoader;
		this.whiteListedRootWithVersion = WHITE_LISTED_ROOT + ".version" + classLoader.getVerificationVersion() + ".";
		ensureVerificationVersionExistsInDatabase();
	}

	@Override
	public Optional<Field> whiteListingModelOf(Field field) {
		// if the class defining the field has been loaded by this class loader,
		// then it comes from the store of the node and the field is white-listed
		if (field.getDeclaringClass().getClassLoader() == classLoader)
			return Optional.of(field);
		else
			return fieldInWhiteListedLibraryFor(field);
	}

	@Override
	public Optional<Constructor<?>> whiteListingModelOf(Constructor<?> constructor) {
		// if the class defining the constructor has been loaded by this class loader,
		// then it comes from the store of the node and the constructor is white-listed
		if (constructor.getDeclaringClass().getClassLoader() == classLoader)
			return Optional.of(constructor);
		else
			return constructorInWhiteListedLibraryFor(constructor);
	}

	@Override
	public Optional<Method> whiteListingModelOf(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();

		if (declaringClass.getClassLoader() == classLoader)
			// if the class defining the method has been loaded by this class loader,
			// then it comes from the store of the node and the method is white-listed
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
					try {
						Optional<Method> overridden = classLoader.resolveMethod(superclass, method.getName(), method.getParameterTypes(), method.getReturnType());
						if (overridden.isPresent()) {
							result = whiteListingModelOf(overridden.get());
							if (result.isPresent())
								return result;
						}
					}
					catch (ClassNotFoundException e) {}
				}

				for (Class<?> superinterface: declaringClass.getInterfaces()) {
					try {
						Optional<Method> overridden = classLoader.resolveMethod(superinterface, method.getName(), method.getParameterTypes(), method.getReturnType());
						if (overridden.isPresent()) {
							result = whiteListingModelOf(overridden.get());
							if (result.isPresent())
								return result;
						}
					}
					catch (ClassNotFoundException e) {}
				}
			}
		}

		return Optional.empty();
	}

	/**
	 * Ensures that the required version of the white-listing annotations is present.
	 * 
	 * @throws UnsupportedVerificationVersionException if the annotations for the required verification
	 *                                                 version cannot be found in the database
	 */
	private void ensureVerificationVersionExistsInDatabase() throws UnsupportedVerificationVersionException {
		try {
			classLoader.loadClass(whiteListedRootWithVersion + Object.class.getName());
		}
		catch (ClassNotFoundException e) {
			throw new UnsupportedVerificationVersionException(classLoader.getVerificationVersion());
		}
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

	private Optional<Method> methodInWhiteListedLibraryFor(Method method) {
		// method Object.getClass() is white-listed but we cannot put it in the white-listed library, since that method is final in Object;
		// it is important to have it white-listed since it is invoked implicitly in the code generated by many compilers
		if (method.getDeclaringClass() == Object.class && "getClass".equals(method.getName()))
			try {
				return Optional.of(Object.class.getMethod("getClass"));
			}
			catch (NoSuchMethodException e) {
				// this will never happen, since Object.getClass() exists
				throw new RuntimeException(e);
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