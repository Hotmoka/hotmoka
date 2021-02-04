package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.RedPayableNotInRedGreenContractError;
import io.takamaka.code.verification.issues.RedPayableWithoutFromContractError;

/**
 * A check that {@code @@RedPayable} is applied only to from contract code of red/green contracts.
 */
public class RedPayableCodeIsFromContractOfRedGreenContractCheck extends CheckOnMethods {

	public RedPayableCodeIsFromContractOfRedGreenContractCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (annotations.isRedPayable(className, methodName, methodArgs, methodReturnType)) {
			if (!annotations.isFromContract(className, methodName, methodArgs, methodReturnType))
				issue(new RedPayableWithoutFromContractError(inferSourceFile(), methodName));

			if (!classLoader.isRedGreenContract(className))
				issue(new RedPayableNotInRedGreenContractError(inferSourceFile(), methodName));
		}
	}
}