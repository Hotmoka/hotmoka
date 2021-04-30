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

package io.takamaka.code.verification.internal.checksOnMethods;

import java.math.BigInteger;

import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.RedPayableWithoutAmountError;

/**
 * A checks that red payable methods have an amount first argument.
 */
public class RedPayableCodeReceivesAmountCheck extends CheckOnMethods {

	public RedPayableCodeReceivesAmountCheck(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

		if (annotations.isRedPayable(className, methodName, methodArgs, methodReturnType) && !startsWithAmount())
			issue(new RedPayableWithoutAmountError(inferSourceFile(), methodName));
	}

	private final static ObjectType BIG_INTEGER_OT = new ObjectType(BigInteger.class.getName());

	private boolean startsWithAmount() {
		return methodArgs.length > 0 && (methodArgs[0] == Type.INT || methodArgs[0] == Type.LONG || BIG_INTEGER_OT.equals(methodArgs[0]));
	}
}