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

package io.hotmoka.whitelisting.api;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * A class loader that implements resolution methods for fields, constructors and methods,
 * according to Java's resolution rules.
 */
public interface WhiteListingClassLoader {

	/**
	 * Yields the version of the verification module that this class loader is using.
	 * 
	 * @return the version of the verification module
	 */
	long getVerificationVersion();

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
	 * @throws ClassNotFoundException if {@code className} cannot be found
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
	 * No look-up is performed beyond the starting class.
	 * 
	 * @param className the name of the class declaring the constructor
	 * @param args the arguments of the constructor
	 * @return the resolved constructor, if any
	 * @throws ClassNotFoundException if {@code className} cannot be found
	 */
	Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException;

	/**
	 * Yields the constructor resolved from the given static description.
	 * No look-up is performed beyond the starting class.
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
	 * @throws ClassNotFoundException if {@code className} cannot be found
	 */
	Optional<Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException;

	/**
	 * Yields the method resolved from the given static description.
	 * 
	 * @param clazz the class from which the method look-up must start
	 * @param methodName the name of the method
	 * @param args the arguments of the method
	 * @param returnType the return type of the method
	 * @return the resolved method, if any. It is defined in {@code className} or in one of its superclasses or implemented interfaces
	 */
	Optional<Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType);

	/**
	 * Yields the interface method resolved from the given static description.
	 * 
	 * @param className the name of the class from which the method look-up must start
	 * @param methodName the name of the method
	 * @param args the arguments of the method
	 * @param returnType the return type of the method
	 * @return the resolved method, if any. It is defined in {@code className} or in one of its implemented interfaces
	 * @throws ClassNotFoundException if {@code className} cannot be found
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