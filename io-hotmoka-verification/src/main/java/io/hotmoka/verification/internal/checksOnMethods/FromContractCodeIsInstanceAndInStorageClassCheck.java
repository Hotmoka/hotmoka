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

package io.hotmoka.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.api.UnknownTypeException;
import io.hotmoka.verification.errors.FromContractNotInStorageError;
import io.hotmoka.verification.errors.IllegalFromContractArgumentError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that {@code @@FromContract} is applied only to instance methods or constructors of storage classes or interfaces.
 */
public class FromContractCodeIsInstanceAndInStorageClassCheck extends CheckOnMethods {

	public FromContractCodeIsInstanceAndInStorageClassCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws UnknownTypeException {
		super(builder, method);

		getMethodFromContractArgumentIn(className).ifPresent(fromContractArgument -> {
			if (!classLoader.getContract().isAssignableFrom(fromContractArgument))
				issue(new IllegalFromContractArgumentError(inferSourceFile(), method));

			if (method.isStatic())
				issue(new FromContractNotInStorageError(inferSourceFile(), methodName));

			if (!isInterface && !isStorage)
				issue(new FromContractNotInStorageError(inferSourceFile(), methodName));
		});
	}
}