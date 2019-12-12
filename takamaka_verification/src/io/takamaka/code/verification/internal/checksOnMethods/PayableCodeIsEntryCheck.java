package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.PayableWithoutEntryError;

/**
 * A check that {@code @@Payable} is applied only to entries.
 */
public class PayableCodeIsEntryCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public PayableCodeIsEntryCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (annotations.isPayable(className, methodName, methodArgs, methodReturnType)
				&& !annotations.isEntry(className, methodName, methodArgs, methodReturnType))
			issue(new PayableWithoutEntryError(inferSourceFile(), methodName));
	}
}