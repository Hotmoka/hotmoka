package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.PayableWithRedPayableError;

/**
 * A check that {@code @@Payable} and {@code @@RedPayable} are not applied together to the same
 * method or constructor.
 */
public class PayableCodeIsNotRedPayableCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public PayableCodeIsNotRedPayableCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (annotations.isPayable(className, methodName, methodArgs, methodReturnType)
				&& annotations.isRedPayable(className, methodName, methodArgs, methodReturnType))
			issue(new PayableWithRedPayableError(inferSourceFile(), methodName));
	}
}