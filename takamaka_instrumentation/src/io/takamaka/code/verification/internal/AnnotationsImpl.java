package io.takamaka.code.verification.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.instrumentation.Dummy;
import io.takamaka.code.verification.Annotations;
import io.takamaka.code.verification.Constants;
import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.VerifiedJar;

/**
 * A utility to check the annotations of the methods in a given jar.
 */
public class AnnotationsImpl implements Annotations {
	private final static ObjectType CONTRACT_OT = new ObjectType(Constants.CONTRACT_NAME);
	private final static ObjectType DUMMY_OT = new ObjectType(Dummy.class.getName());

	/**
	 * The jar whose annotations are considered.
	 */
	private final VerifiedJar jar;

	/**
	 * Builds the utility object.
	 * 
	 * @param clazz the jar whose annotations are considered 
	 */
	AnnotationsImpl(VerifiedJar jar) {
		this.jar = jar;
	}

	@Override
	public final boolean isPayable(String className, String methodName, Type[] formals, Type returnType) {
		return getAnnotation(className, methodName, formals, returnType, Constants.PAYABLE_NAME).isPresent();
	}

	@Override
	public final boolean isThrowsExceptions(String className, String methodName, Type[] formals, Type returnType) {
		return getAnnotation(className, methodName, formals, returnType, Constants.THROWS_EXCEPTIONS_NAME).isPresent();
	}

	@Override
	public final Optional<Class<?>> isEntry(String className, String methodName, Type[] formals, Type returnType) {
		Optional<Annotation> annotation = getAnnotation(className, methodName, formals, returnType, Constants.ENTRY_NAME);
		if (annotation.isPresent()) {
			Annotation entry = annotation.get();
			// we call, by reflection, its value() method, to find the type of the calling contract

			Class<?> contractClass;
			try {
				Method value = entry.getClass().getMethod("value");
				contractClass = (Class<?>) value.invoke(entry);
			}
			catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				return Optional.empty();
			}

			return Optional.of(contractClass != null && contractClass != Object.class ? contractClass : jar.getClassLoader().getContract());
		}

		return Optional.empty();
	}

	@Override
	public final boolean isEntryPossiblyAlreadyInstrumented(String className, String methodName, String signature) {
		Type[] formals = Type.getArgumentTypes(signature);
		Type returnType = Type.getReturnType(signature);
		if (isEntry(className, methodName, formals, returnType).isPresent())
			return true;

		// the method might have been already instrumented, since it comes from
		// a jar already installed in blockchain; hence we try with the extra parameters added by instrumentation
		Type[] formalsExpanded = new Type[formals.length + 2];
		System.arraycopy(formals, 0, formalsExpanded, 0, formals.length);
		formalsExpanded[formals.length] = CONTRACT_OT;
		formalsExpanded[formals.length + 1] = DUMMY_OT;
		return isEntry(className, methodName, formalsExpanded, returnType).isPresent();
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
	 * @param annotation the class token of the annotation
	 * @return the annotation, if any
	 */
	private Optional<Annotation> getAnnotation(String className, String methodName, Type[] formals, Type returnType, String annotationName) {
		if (methodName.equals(Const.CONSTRUCTOR_NAME))
			return getAnnotationOfConstructor(className, formals, annotationName);
		else
			return getAnnotationOfMethod(className, methodName, formals, returnType, annotationName);
	}

	private Optional<Annotation> getAnnotationOfConstructor(String className, Type[] formals, String annotationName) {
		Class<?>[] formalsClass = Stream.of(formals).map(jar.getBcelToClass()::of).toArray(Class[]::new);

		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() ->
			Stream.of(jar.getClassLoader().loadClass(className).getDeclaredConstructors())
				.filter(constructor -> Arrays.equals(constructor.getParameterTypes(), formalsClass))
				.flatMap(constructor -> Stream.of(constructor.getAnnotations()))
				.filter(annotation -> annotation.annotationType().getName().equals(annotationName))
				.findFirst());
	}

	private Optional<Annotation> getAnnotationOfMethod(String className, String methodName, Type[] formals, Type returnType, String annotationName) {
		Class<?> returnTypeClass = jar.getBcelToClass().of(returnType);
		Class<?>[] formalsClass = Stream.of(formals).map(jar.getBcelToClass()::of).toArray(Class[]::new);

		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Class<?> clazz = jar.getClassLoader().loadClass(className);
			Optional<Method> definition = Stream.of(clazz.getDeclaredMethods())
				.filter(m -> m.getName().equals(methodName) && m.getReturnType() == returnTypeClass && Arrays.equals(m.getParameterTypes(), formalsClass))
				.findFirst();

			if (definition.isPresent())
				return Stream.of(definition.get().getAnnotations())
					.filter(annotation -> annotation.annotationType().getName().equals(annotationName))
					.findFirst();

			return Stream.concat(Stream.of(clazz.getSuperclass()), Stream.of(clazz.getInterfaces()))
				.filter(where -> where != null) // since the superclass might be null
				.map(where -> getAnnotationOfMethod(where.getName(), methodName, formals, returnType, annotationName))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();
		});
	}
}