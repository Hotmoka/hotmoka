package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.PayableNotInContractError;
import io.takamaka.code.verification.issues.PayableWithoutFromContractError;

/**
 * A check that {@code @@Payable} is applied only to from contract code of contracts.
 */
public class PayableCodeIsFromContractCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public PayableCodeIsFromContractCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (annotations.isPayable(className, methodName, methodArgs, methodReturnType)) {
			if (!annotations.isFromContract(className, methodName, methodArgs, methodReturnType))
				issue(new PayableWithoutFromContractError(inferSourceFile(), methodName));

			if (!classLoader.isContract(className))
				issue(new PayableNotInContractError(inferSourceFile(), methodName));
		}
	}
}