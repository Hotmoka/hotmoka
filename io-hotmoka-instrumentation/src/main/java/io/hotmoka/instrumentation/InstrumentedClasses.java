/*
Copyright 2023 Fausto Spoto

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
import io.hotmoka.instrumentation.api.InstrumentedClass;
import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.api.VerifiedClass;

/**
 * A supplier of instrumented classes.
 */
public final class InstrumentedClasses {

	private InstrumentedClasses () {}

	/**
	 * Yields an instrumented class from a verified class.
	 * 
	 * @param clazz the verified class to instrument
	 * @param gasCostModel the gas cost model used for the instrumentation
	 * @return the instrumented class
	 * @throws IllegalJarException if the jar under instrumentation, containing this class, is illegal
	 * @throws UnknownTypeException if some type cannot be resolved
	 */
	public static InstrumentedClass of(VerifiedClass clazz, GasCostModel gasCostModel) throws IllegalJarException, UnknownTypeException {
		return new InstrumentedClassImpl(clazz, gasCostModel);
	}
}