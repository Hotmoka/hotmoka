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

import static io.hotmoka.exceptions.CheckSupplier.check2;
import static io.hotmoka.exceptions.UncheckFunction.uncheck;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.hotmoka.constants.Constants;
import io.hotmoka.verification.Dummy;
import io.hotmoka.verification.api.Annotations;

/**
 * A utility to check the annotations of the methods in a given jar.
 */
public class AnnotationsImpl implements Annotations {
	private final static ObjectType CONTRACT_OT = new ObjectType(Constants.CONTRACT_NAME);
	private final static ObjectType DUMMY_OT = new ObjectType(Dummy.class.getName());

	/**
	 * The jar whose annotations are considered.
	 */
	private final VerifiedJarImpl jar;

	/**
	 * Builds the utility object.
	 * 
	 * @param jar the jar whose annotations are considered
	 */
	AnnotationsImpl(VerifiedJarImpl jar) {
		this.jar = jar;
	}

	@Override
	public final boolean isPayable(String className, String methodName, Type[] formals, Type returnType) throws SecurityException, ClassNotFoundException {
		return getAnnotation(className, methodName, formals, returnType, Constants.PAYABLE_NAME).isPresent()
			|| getAnnotation(className, methodName, expandFormals(formals), returnType, Constants.PAYABLE_NAME).isPresent();
	}

	@Override
	public final boolean isRedPayable(String className, String methodName, Type[] formals, Type returnType) throws SecurityException, ClassNotFoundException {
		return getAnnotation(className, methodName, formals, returnType, Constants.RED_PAYABLE_NAME).isPresent()
			|| getAnnotation(className, methodName, expandFormals(formals), returnType, Constants.RED_PAYABLE_NAME).isPresent();
	}

	@Override
	public final boolean isSelfCharged(String className, String methodName, Type[] formals, Type returnType) throws SecurityException, ClassNotFoundException {
		return getAnnotation(className, methodName, formals, returnType, Constants.SELF_CHARGED_NAME).isPresent()
			|| getAnnotation(className, methodName, expandFormals(formals), returnType, Constants.SELF_CHARGED_NAME).isPresent();
	}

	@Override
	public final boolean isThrowsExceptions(String className, String methodName, Type[] formals, Type returnType) throws SecurityException, ClassNotFoundException {
		return getAnnotation(className, methodName, formals, returnType, Constants.THROWS_EXCEPTIONS_NAME).isPresent()
			|| getAnnotation(className, methodName, expandFormals(formals), returnType, Constants.THROWS_EXCEPTIONS_NAME).isPresent();
	}

	@Override
	public final boolean isFromContract(String className, String methodName, Type[] formals, Type returnType) throws SecurityException, ClassNotFoundException {
		return getFromContractArgument(className, methodName, formals, returnType).isPresent();
	}

	@Override
	public final Optional<Class<?>> getFromContractArgument(String className, String methodName, Type[] formals, Type returnType) throws SecurityException, ClassNotFoundException {
		Optional<Annotation> annotation = getAnnotation(className, methodName, formals, returnType, Constants.FROM_CONTRACT_NAME);
		if (annotation.isEmpty())
			// the method might have been already instrumented, since it comes from
			// a jar already installed in blockchain; hence we try with the extra parameters added by instrumentation
			annotation = getAnnotation(className, methodName, expandFormals(formals), returnType, Constants.FROM_CONTRACT_NAME);

		return annotation.map(this::extractContractClass);
	}

	/**
	 * Adds, to the given formal arguments, the extra two used in instrumented entries.
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

	private Class<?> extractContractClass(Annotation entry) {
		// we call, by reflection, its value() method, to find the type of the calling contract

		Class<?> contractClass;
		try {
			Method value = entry.getClass().getMethod("value");
			contractClass = (Class<?>) value.invoke(entry);
		}
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			return null;
		}

		return contractClass != null ? contractClass : jar.classLoader.getContract();
	}

	/**
	 * Gets the given annotation from the given constructor or method. For methods, looks
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
	 * @throws SecurityException 
	 */
	private Optional<Annotation> getAnnotation(String className, String methodName, Type[] formals, Type returnType, String annotationName) throws SecurityException, ClassNotFoundException {
		if (methodName.equals(Const.CONSTRUCTOR_NAME))
			return getAnnotationOfConstructor(className, formals, annotationName);
		else
			return getAnnotationOfMethod(className, methodName, formals, returnType, annotationName);
	}

	private Optional<Annotation> getAnnotationOfConstructor(String className, Type[] formals, String annotationName) throws ClassNotFoundException {
		Class<?>[] formalsClass = check2(ClassNotFoundException.class, () ->
			Stream.of(formals).map(uncheck(jar.bcelToClass::of)).toArray(Class[]::new)
		);

		return Stream.of(jar.classLoader.loadClass(className).getDeclaredConstructors())
				.filter(constructor -> Arrays.equals(constructor.getParameterTypes(), formalsClass))
				.flatMap(constructor -> Stream.of(constructor.getAnnotations()))
				.filter(annotation -> annotation.annotationType().getName().equals(annotationName))
				.findFirst();
	}

	private Optional<Annotation> getAnnotationOfMethod(String className, String methodName, Type[] formals, Type returnType, String annotationName) throws ClassNotFoundException {
		Class<?> returnTypeClass = jar.bcelToClass.of(returnType);
		Class<?>[] formalsClass = check2(ClassNotFoundException.class, () ->
			Stream.of(formals).map(uncheck(jar.bcelToClass::of)).toArray(Class[]::new)
		);

		Class<?> clazz = jar.classLoader.loadClass(className);
		Optional<Method> definition = Stream.of(clazz.getDeclaredMethods())
			.filter(m -> m.getName().equals(methodName) && m.getReturnType() == returnTypeClass && Arrays.equals(m.getParameterTypes(), formalsClass))
			.findFirst();

		if (definition.isPresent()) {
			Method method = definition.get();
			Optional<Annotation> explicit = Stream.of(method.getAnnotations())
				.filter(annotation -> annotation.annotationType().getName().equals(annotationName))
				.findFirst();

			if (explicit.isPresent())
				return explicit;

			Class<?> superclass;
			if ((superclass = clazz.getSuperclass()) != null && method.isBridge() && method.isSynthetic() && !Modifier.isPrivate(method.getModifiers()))
				// bridge synthetic methods are created by compilers to override a method of the superclass,
				// but they do not put the same annotations as in the superclass while it should be the case
				return getAnnotationOfMethod(superclass.getName(), methodName, formals, returnType, annotationName);

			return Optional.empty();
		}
	
		return check2(ClassNotFoundException.class, () ->
			Stream.concat(Stream.of(clazz.getSuperclass()), Stream.of(clazz.getInterfaces()))
				.filter(Objects::nonNull) // since the superclass might be null
				.map(uncheck(where -> getAnnotationOfMethod(where.getName(), methodName, formals, returnType, annotationName)))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst()
			);
	}
}