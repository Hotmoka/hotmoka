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

package io.hotmoka.verification;

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
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isPayable(String className, String methodName, Type[] formals, Type returnType) throws ClassNotFoundException;

	/**
	 * Determines if the given constructor or method is annotated as red payable.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isRedPayable(String className, String methodName, Type[] formals, Type returnType) throws ClassNotFoundException;

	/**
	 * Determines if the given constructor or method is annotated as self charged.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isSelfCharged(String className, String methodName, Type[] formals, Type returnType) throws ClassNotFoundException;

	/**
	 * Determines if the given constructor or method is annotated as {@code @@ThrowsExceptions}.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isThrowsExceptions(String className, String methodName, Type[] formals, Type returnType) throws ClassNotFoundException;

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
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	Optional<Class<?>> getFromContractArgument(String className, String methodName, Type[] formals, Type returnType) throws ClassNotFoundException;

	/**
	 * Determines if the given constructor or method is annotated as {@code @@FromContract}.
	 * 
	 * @param className the class of the constructor or method
	 * @param methodName the name of the constructor or method
	 * @param formals the types of the formal arguments of the method
	 * @param returnType the return type of the method
	 * @return true if and only if that condition holds
	 * @throws ClassNotFoundException if {@code className} cannot be found in the Takamaka program
	 */
	boolean isFromContract(String className, String methodName, Type[] formals, Type returnType) throws ClassNotFoundException;
}