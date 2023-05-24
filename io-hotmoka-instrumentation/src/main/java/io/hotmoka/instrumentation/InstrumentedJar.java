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

package io.hotmoka.instrumentation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.hotmoka.instrumentation.internal.InstrumentedJarImpl;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.VerifiedJar;

/**
 * An instrumented jar file, built from another, verified jar file. This means
 * for instance that storage classes get modified to account for persistence and
 * contracts get modified to implement entries.
 */
public interface InstrumentedJar {

	/**
	 * Instruments the given jar file into another jar file. This instrumentation
	 * will fail if at least a class did not verify.
	 * 
	 * @param verifiedJar the jar that contains the classes already verified
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be found
	 * @throws VerificationException if {@code verifiedJar} has some error
	 */
	static InstrumentedJar of(VerifiedJar verifiedJar, GasCostModel gasCostModel) throws ClassNotFoundException {
		return new InstrumentedJarImpl(verifiedJar, gasCostModel);
	}

	/**
	 * Dumps this instrumented jar into a file.
	 * 
	 * @param destination the destination file where the jar must be dumped
	 * @throws IOException if an I/O error occurred
	 */
	void dump(Path destination) throws IOException;

	/**
	 * Yields the bytes of this jar.
	 * 
	 * @return the bytes
	 */
	byte[] toBytes();

	/**
	 * Yields the classes in this jar file.
	 * 
	 * @return the classes
	 */
	Stream<InstrumentedClass> classes();
}