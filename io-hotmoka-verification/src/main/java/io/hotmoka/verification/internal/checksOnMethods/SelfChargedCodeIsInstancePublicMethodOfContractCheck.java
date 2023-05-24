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

import org.apache.bcel.Const;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.errors.InconsistentSelfChargedError;
import io.hotmoka.verification.errors.SelfChargedNotAllowedError;
import io.hotmoka.verification.internal.CheckOnMethods;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A checks that {@code @@SelfCharged} is applied to instance public methods of contracts only.
 */
public class SelfChargedCodeIsInstancePublicMethodOfContractCheck extends CheckOnMethods {

	public SelfChargedCodeIsInstancePublicMethodOfContractCheck(VerifiedClassImpl.Verification builder, MethodGen method) throws ClassNotFoundException {
		super(builder, method);

		if (annotations.isSelfCharged(className, methodName, methodArgs, methodReturnType))
			if (!allowSelfCharged)
				issue(new SelfChargedNotAllowedError(inferSourceFile(), methodName));
			else if (!isInPublicInstanceMethodOfContract())
				issue(new InconsistentSelfChargedError(inferSourceFile(), methodName));
	}

	private boolean isInPublicInstanceMethodOfContract() {
		return method.isPublic() && !method.isStatic() && !Const.CONSTRUCTOR_NAME.equals(method.getName()) && classLoader.isContract(className);
	}
}