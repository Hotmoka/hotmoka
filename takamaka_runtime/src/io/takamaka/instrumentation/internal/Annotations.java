package io.takamaka.instrumentation.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.instrumentation.Dummy;
import takamaka.blockchain.types.ClassType;

/**
 * A utility to check the annotations of the methods in a given class.
 */
public class Annotations {
	private final static ObjectType CONTRACT_OT = new ObjectType(ClassType.CONTRACT.name);
	private final static ObjectType DUMMY_OT = new ObjectType(Dummy.class.getName());

	/**
	 * The class whose annotations are considered.
	 */
	private final VerifiedClass clazz;

	/**
	 * Builds the utility object.
	 * 
	 * @param clazz the class whose annotations are considered 
	 */
	Annotations(VerifiedClass clazz) {
		this.clazz = clazz;
	}

	/**
	 * Determines if the given constructor or method is annotated as payable.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 */
	public final boolean isPayable(String className, String methodName, Type[] formals, Type returnType) {
		return getAnnotation(className, methodName, formals, returnType, ClassType.PAYABLE.name).isPresent();
	}

	/**
	 * Determines if the given constructor or method is annotated as {@code @@ThrowsExceptions}.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 */
	public final boolean isThrowsExceptions(String className, String methodName, Type[] formals, Type returnType) {
		return getAnnotation(className, methodName, formals, returnType, ClassType.THROWS_EXCEPTIONS.name).isPresent();
	}

	/**
	 * Determines if the given constructor or method is annotated as entry.
	 * Yields the argument of the annotation.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return the value of the annotation, if any. For instance, for {@code @@Entry(PayableContract.class)}
	 *         this return value will be {@code takamaka.lang.PayableContract.class}
	 */
	public final Optional<Class<?>> isEntry(String className, String methodName, Type[] formals, Type returnType) {
		Optional<Annotation> annotation = getAnnotation(className, methodName, formals, returnType, ClassType.ENTRY.name);
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

			return Optional.of(contractClass != null && contractClass != Object.class ? contractClass : clazz.classLoader.contractClass);
		}

		return Optional.empty();
	}

	/**
	 * Determines if a method is an entry, possibly already instrumented.
	 * 
	 * @param className the name of the class defining the method
	 * @param methodName the name of the method
	 * @param signature the signature of the method
	 * @return true if and only if that condition holds
	 */
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
		Class<?>[] formalsClass = Stream.of(formals).map(clazz.classLoader::bcelToClass).toArray(Class[]::new);

		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() ->
			Stream.of(clazz.classLoader.loadClass(className).getDeclaredConstructors())
				.filter(constructor -> Arrays.equals(constructor.getParameterTypes(), formalsClass))
				.flatMap(constructor -> Stream.of(constructor.getAnnotations()))
				.filter(annotation -> annotation.annotationType().getName().equals(annotationName))
				.findFirst());
	}

	private Optional<Annotation> getAnnotationOfMethod(String className, String methodName, Type[] formals, Type returnType, String annotationName) {
		Class<?> returnTypeClass = clazz.classLoader.bcelToClass(returnType);
		Class<?>[] formalsClass = Stream.of(formals).map(clazz.classLoader::bcelToClass).toArray(Class[]::new);

		return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
			Class<?> clazz = this.clazz.classLoader.loadClass(className);
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