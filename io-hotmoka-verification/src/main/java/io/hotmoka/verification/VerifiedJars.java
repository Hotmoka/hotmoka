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

import java.util.function.Consumer;

import io.hotmoka.exceptions.ExceptionReplacer;
import io.hotmoka.verification.api.Error;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerificationException;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.verification.internal.VerifiedJarImpl;

/**
 * A provider of jars that have undergone static verification, before being installed into blockchain.
 */
public abstract class VerifiedJars {

	private VerifiedJars() {}

	/**
	 * Creates a verified jar from the given file. Calls the given task for each error
	 * generated during the verification. At the end, throws an exception if there is at least an error.
	 * 
	 * @param jar the jar file to verify, given as an array of bytes
	 * @param classLoader the class loader that can be used to resolve the classes of the program, including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during the node initialization
	 * @param onError a task to execute for each error found during the verification
	 * @param skipsVerification true if and only if the static verification of the classes of the jar must be skipped
	 * @return the verified jar
	 * @throws VerificationException if verification fails
	 * @throws IllegalJarException if the jar under verification is illegal
	 * @throws UnknownTypeException if some type of the jar under verification cannot be resolved
	 */
	public static VerifiedJar of(byte[] jar, TakamakaClassLoader classLoader, boolean duringInitialization, Consumer<Error> onError, boolean skipsVerification) throws VerificationException, IllegalJarException, UnknownTypeException {
		return new VerifiedJarImpl(jar, classLoader, duringInitialization, onError, skipsVerification);
	}

	/**
	 * Creates a verified jar from the given file. Calls the given task for each error
	 * generated during the verification. At the end, throws an exception if there is at least an error.
	 * 
	 * @param <E1> the exception thrown if verification fails
	 * @param <E2> the exception thrown if the jar under verification is illegal
	 * @param <E3> the exception thrown if the jar under verification refers to a type that cannot be resolved
	 * @param jar the jar file to verify, given as an array of bytes
	 * @param classLoader the class loader that can be used to resolve the classes of the program, including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during the node initialization
	 * @param onError a task to execute for each error found during the verification
	 * @param skipsVerification true if and only if the static verification of the classes of the jar must be skipped
	 * @param verificationExceptionReplacer a replacer of the verification exception
	 * @param illegalJarExceptionReplacer a replacer of the illegal jar exception
	 * @param unknownTypeExceptionReplacer a replacer of the unknown type exception
	 * @return the verified jar
	 * @throws E1 if verification fails
	 * @throws E2 if the jar under verification is illegal
	 * @throws E3 if the jar under verification refers to a type that cannot be resolved
	 */
	public static <E1 extends Exception, E2 extends Exception, E3 extends Exception> VerifiedJar of(byte[] jar, TakamakaClassLoader classLoader, boolean duringInitialization, Consumer<Error> onError, boolean skipsVerification,
			ExceptionReplacer<? super VerificationException, ? extends E1> verificationExceptionReplacer,
			ExceptionReplacer<? super IllegalJarException, ? extends E2> illegalJarExceptionReplacer,
			ExceptionReplacer<? super UnknownTypeException, ? extends E3> unknownTypeExceptionReplacer) throws E1, E2, E3 {

		try {
			return new VerifiedJarImpl(jar, classLoader, duringInitialization, onError, skipsVerification);
		}
		catch (VerificationException e) {
			throw verificationExceptionReplacer.apply(e);
		}
		catch (IllegalJarException e) {
			throw illegalJarExceptionReplacer.apply(e);
		}
		catch (UnknownTypeException e) {
			throw unknownTypeExceptionReplacer.apply(e);
		}
	}
}