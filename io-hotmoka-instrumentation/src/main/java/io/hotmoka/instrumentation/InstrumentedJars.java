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

import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.instrumentation.api.InstrumentedJar;
import io.hotmoka.instrumentation.internal.InstrumentedJarImpl;
import io.hotmoka.verification.VerificationException;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.VerifiedJar;

/**
 * Supplier of instrumented jars.
 */
public final class InstrumentedJars {

	private InstrumentedJars() {}

	/**
	 * Yields an instrumented jar file from a jar file that already passed verification.
	 * Instrumentation will fail if at least a class does not verify.
	 * 
	 * @param verifiedJar the already verified jar
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @return the instrumented jar
	 * @throws IllegalJarException if {@code verifiedJar} is illegal
	 * @throws VerificationException if {@code verifiedJar} has some verification error
	 */
	public static InstrumentedJar of(VerifiedJar verifiedJar, GasCostModel gasCostModel) throws IllegalJarException, VerificationException {
		return new InstrumentedJarImpl(verifiedJar, gasCostModel);
	}
}