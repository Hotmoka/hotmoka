package io.takamaka.code.verification;

import java.util.Optional;

import org.apache.bcel.generic.Type;

/**
 * A utility to check the annotations of the methods in a given jar.
 */
public interface Annotations {

	/**
	 * Determines if the given constructor or method is annotated as payable.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 */
	boolean isPayable(String className, String methodName, Type[] formals, Type returnType);

	/**
	 * Determines if the given constructor or method is annotated as red payable.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 */
	boolean isRedPayable(String className, String methodName, Type[] formals, Type returnType);

	/**
	 * Determines if the given constructor or method is annotated as {@code @@ThrowsExceptions}.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 */
	boolean isThrowsExceptions(String className, String methodName, Type[] formals, Type returnType);

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
	Optional<Class<?>> isEntry(String className, String methodName, Type[] formals, Type returnType);

	/**
	 * Determines if a method is an entry, possibly already instrumented.
	 * 
	 * @param className the name of the class defining the method
	 * @param methodName the name of the method
	 * @param signature the signature of the method
	 * @return true if and only if that condition holds
	 */
	boolean isEntryPossiblyAlreadyInstrumented(String className, String methodName, String signature);
}