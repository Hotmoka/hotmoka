package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.PayableNotInContractError;
import io.takamaka.code.verification.issues.PayableWithoutEntryError;

/**
 * A check that {@code @@Payable} is applied only to entries of contracts.
 */
public class PayableCodeIsEntryOfContractCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public PayableCodeIsEntryOfContractCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (annotations.isPayable(className, methodName, methodArgs, methodReturnType)) {
			if (!annotations.isEntry(className, methodName, methodArgs, methodReturnType))
				issue(new PayableWithoutEntryError(inferSourceFile(), methodName));

			if (!classLoader.isContract(className))
				issue(new PayableNotInContractError(inferSourceFile(), methodName));
		}
	}
}