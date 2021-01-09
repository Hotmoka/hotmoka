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
	 * Determines if the given constructor or method is annotated as self charged.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 */
	boolean isSelfCharged(String className, String methodName, Type[] formals, Type returnType);

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
	 * Determines the argument of the {@code @@FromContract} annotation of the given constructor or method, if any.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return the argument of the annotation, if any. For instance, for {@code @@FromContract(PayableContract.class)}
	 *         this return value will be {@code takamaka.lang.PayableContract.class}. If no argument is specified,
	 *         the result is {@code io.takamaka.code.lang.Contract}. If the argument cannot be determined, the result
	 *         is an empty optional
	 */
	Optional<Class<?>> getFromContractArgument(String className, String methodName, Type[] formals, Type returnType);

	/**
	 * Determines if the given constructor or method is annotated as {@code @@FromContract}.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 */
	boolean isFromContract(String className, String methodName, Type[] formals, Type returnType);
}