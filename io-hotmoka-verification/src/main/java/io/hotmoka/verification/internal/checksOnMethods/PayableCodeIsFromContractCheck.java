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
import io.hotmoka.verification.errors.PayableNotInContractError;
import io.hotmoka.verification.errors.PayableWithoutFromContractError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that {@code @@Payable} is applied only to from contract code of contracts.
 */
public class PayableCodeIsFromContractCheck extends CheckOnMethods {

	public PayableCodeIsFromContractCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws UnknownTypeException {
		super(builder, method);

		if (methodIsPayableIn(className)) {
			if (!methodIsFromContractIn(className))
				issue(new PayableWithoutFromContractError(inferSourceFile(), method));

			if (!isContract && !isInterface)
				issue(new PayableNotInContractError(inferSourceFile(), method));
		}
	}
}