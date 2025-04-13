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

package io.hotmoka.verification.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.verification.BcelToClassTransformers;
import io.hotmoka.verification.api.AnnotationUtility;
import io.hotmoka.verification.api.BcelToClassTransformer;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.whitelisting.WhitelistingConstants;
import io.takamaka.code.constants.Constants;

/**
 * A utility to check the annotations of the methods in a given jar.
 */
public class AnnotationUtilityImpl implements AnnotationUtility {
	private final static ObjectType CONTRACT_OT = new ObjectType(Constants.CONTRACT_NAME);
	private final static ObjectType DUMMY_OT = new ObjectType(WhitelistingConstants.DUMMY_NAME);

	/**
	 * The class loader of the jar whose annotations are considered.
	 */
	private final TakamakaClassLoader classLoader;

	/**
	 * The utility used to transform BCEL types into classes.
	 */
	private final BcelToClassTransformer bcelToClass;

	/**
	 * Builds the utility object.
	 * 
	 * @param classLoader the class loader used to load the classes whose annotations are checked
	 */
	public AnnotationUtilityImpl(TakamakaClassLoader classLoader) {
		this.classLoader = classLoader;
		this.bcelToClass = BcelToClassTransformers.of(classLoader);
	}

	@Override
	public final boolean isPayable(String className, String methodName, Type[] formals, Type returnType) throws UnknownTypeException {
		return getAnnotation(className, methodName, formals, returnType, Constants.PAYABLE_NAME).isPresent()
			|| getAnnotation(className, methodName, expandFormals(formals), returnType, Constants.PAYABLE_NAME).isPresent();
	}

	@Override
	public final boolean isWhiteListedDuringInitialization(String className) throws UnknownTypeException {
		return classIsAnnotatedAs(className, Constants.WHITE_LISTED_DURING_INITIALIZATION_NAME);
	}

	@Override
	public final boolean isThrowsExceptions(String className, String methodName, Type[] formals, Type returnType) throws UnknownTypeException {
		return getAnnotation(className, methodName, formals, returnType, Constants.THROWS_EXCEPTIONS_NAME).isPresent()
			|| getAnnotation(className, methodName, expandFormals(formals), returnType, Constants.THROWS_EXCEPTIONS_NAME).isPresent();
	}

	@Override
	public final boolean isFromContract(String className, String methodName, Type[] formals, Type returnType) throws UnknownTypeException {
		return getFromContractArgument(className, methodName, formals, returnType).isPresent();
	}

	@Override
	public final Optional<Class<?>> getFromContractArgument(String className, String methodName, Type[] formals, Type returnType) throws UnknownTypeException {
		Optional<Annotation> annotation = getAnnotation(className, methodName, formals, returnType, Constants.FROM_CONTRACT_NAME);
		if (annotation.isEmpty())
			// the method might have been already instrumented, since it comes from
			// a jar already installed in blockchain; hence we try with the extra parameters added by instrumentation
			annotation = getAnnotation(className, methodName, expandFormals(formals), returnType, Constants.FROM_CONTRACT_NAME);

		return annotation.map(this::extractContractClass);
	}

	/**
	 * Adds, to the given formal arguments, the extra two used in instrumented {@@FromComtract} code.
	 * 
	 * @param formals the original formals
	 * @return the expanded formals
	 */
	private static Type[] expandFormals(Type[] formals) {
		var formalsExpanded = new Type[formals.length + 2];
		System.arraycopy(formals, 0, formalsExpanded, 0, formals.length);
		formalsExpanded[formals.length] = CONTRACT_OT;
		formalsExpanded[formals.length + 1] = DUMMY_OT;
		return formalsExpanded;
	}

	private Class<?> extractContractClass(Annotation fromContract) {
		// we call, by reflection, its value() method, to find the type of the calling contract
		try {
			Method value = fromContract.getClass().getMethod("value");
			Class<?> contractClass = (Class<?>) value.invoke(fromContract);
			// it defaults to Contract
			return contractClass != null ? contractClass : classLoader.getContract();
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException(e); // this should never happen since the value() method should exist
		}
	}

	/**
	 * Gets the given annotation from the given constructor or method. For methods, it looks
	 * in the given class and, if no such method is found there, looks also in the superclass.
	 * If no annotation is found in the superclass, it looks in the super-interfaces as well.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method or constructor
	 * @param returnType the return type of the method or constructor
	 * @param annotationName the name of the annotation class
	 * @return the annotation, if any
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 */
	private Optional<Annotation> getAnnotation(String className, String methodName, Type[] formals, Type returnType, String annotationName) throws UnknownTypeException {
		if (methodName.equals(Const.CONSTRUCTOR_NAME))
			return getAnnotationOfConstructor(className, formals, annotationName);
		else
			return getAnnotationOfMethod(className, methodName, formals, returnType, annotationName);
	}

	private Optional<Annotation> getAnnotationOfConstructor(String className, Type[] formals, String annotationName) throws UnknownTypeException {
		Class<?>[] formalsClass = bcelToClass.of(formals);
		Class<?> clazz;

		try {
			clazz = classLoader.loadClass(className);
		}
		catch (ClassNotFoundException e) {
			throw new UnknownTypeException(className);
		}

		return Stream.of(clazz.getDeclaredConstructors())
				.filter(constructor -> Arrays.equals(constructor.getParameterTypes(), formalsClass))
				.flatMap(constructor -> Stream.of(constructor.getAnnotations()))
				.filter(annotation -> annotation.annotationType().getName().equals(annotationName))
				.findFirst();
	}

	private boolean classIsAnnotatedAs(String className, String annotationName) throws UnknownTypeException {
		Class<?> clazz;

		try {
			clazz = classLoader.loadClass(className);
		}
		catch (ClassNotFoundException e) {
			throw new UnknownTypeException(className);
		}

		boolean explicitly = Stream.of(clazz.getAnnotations())
			.map(annotation -> annotation.annotationType().getName())
			.anyMatch(annotationName::equals);

		Class<?> superclass;
		return explicitly || ((superclass = clazz.getSuperclass()) != null && classIsAnnotatedAs(superclass.getName(), annotationName));
	}

	private Optional<Annotation> getAnnotationOfMethod(String className, String methodName, Type[] formals, Type returnType, String annotationName) throws UnknownTypeException {
		Class<?> returnTypeClass = bcelToClass.of(returnType);
		Class<?>[] formalsClass = bcelToClass.of(formals);
		Class<?> clazz;

		try {
			clazz = classLoader.loadClass(className);
		}
		catch (ClassNotFoundException e) {
			throw new UnknownTypeException(className);
		}

		Optional<Method> definition;
		try {
			definition = Stream.of(clazz.getDeclaredMethods())
					.filter(m -> m.getName().equals(methodName) && m.getReturnType() == returnTypeClass && Arrays.equals(m.getParameterTypes(), formalsClass))
					.findFirst();
		}
		catch (NoClassDefFoundError e) {
			throw new UnknownTypeException(e.getMessage());
		}

		if (definition.isPresent()) {
			Method method = definition.get();

			for (var annotation: method.getAnnotations())
				if (annotation.annotationType().getName().equals(annotationName))
					return Optional.of(annotation);

			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && method.isBridge() && method.isSynthetic() && !Modifier.isPrivate(method.getModifiers()))
				// bridge synthetic methods are created by compilers to override a method of the superclass,
				// but they do not put the same annotations as in the superclass while it should be the case
				return getAnnotationOfMethod(superclass.getName(), methodName, formals, returnType, annotationName);
		}
		else {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null) {
				Optional<Annotation> result = getAnnotationOfMethod(superclass.getName(), methodName, formals, returnType, annotationName);
				if (result.isPresent())
					return result;
			}

			for (var superinterface: clazz.getInterfaces()) {
				Optional<Annotation> result = getAnnotationOfMethod(superinterface.getName(), methodName, formals, returnType, annotationName);
				if (result.isPresent())
					return result;
			}
		}

		return Optional.empty();
	}
}