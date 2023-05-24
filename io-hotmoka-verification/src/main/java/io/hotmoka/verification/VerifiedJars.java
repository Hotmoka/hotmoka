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

import java.io.IOException;

import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.verification.api.VerifiedJar;
import io.hotmoka.verification.internal.VerifiedJarImpl;

/**
 * A provider of jars that have undergone static verification, before being installed into blockchain.
 */
public interface VerifiedJars {

	/**
	 * Creates a verified jar from the given file. This verification
	 * might fail if at least a class did not verify. In that case, the issues generated
	 * during verification will contain at least an error.
	 * 
	 * @param jar the jar file to verify, given as an array of bytes
	 * @param classLoader the class loader that can be used to resolve the classes of the program,
	 *                    including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during the initialization of the node
	 * @param allowSelfCharged true if and only if {@code @@SelfCharged} methods are allowed
	 * @param skipsVerification true if and only if the static verification of the classes of the jar must be skipped
	 * @throws IOException if there was a problem accessing the classes on disk
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	static VerifiedJar of(byte[] jar, TakamakaClassLoader classLoader, boolean duringInitialization, boolean allowSelfCharged, boolean skipsVerification) throws IOException, ClassNotFoundException {
		return new VerifiedJarImpl(jar, classLoader, duringInitialization, allowSelfCharged, skipsVerification);
	}
}