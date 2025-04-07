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

import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.verification.api.Error;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
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
	 * @param <E> the type of the exception thrown if there is at least an error during verification
	 * @param origin the jar file to verify, given as an array of bytes
	 * @param classLoader the class loader that can be used to resolve the classes of the program, including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during the node initialization
	 * @param onError a task to execute for each error found during the verification
	 * @param skipsVerification true if and only if the static verification of the classes of the jar must be skipped
	 * @param ifError the creator of the exception thrown if there is at least an error during verification
	 * @throws E if verification fails
	 * @throws IllegalJarException if the jar under verification is illegal
	 */
	public static <E extends Exception> VerifiedJar of(byte[] jar, TakamakaClassLoader classLoader, boolean duringInitialization, Consumer<Error> onError, boolean skipsVerification, ExceptionSupplier<? extends E> ifError) throws E, IllegalJarException {
		return new VerifiedJarImpl(jar, classLoader, duringInitialization, onError, skipsVerification, ifError);
	}
}