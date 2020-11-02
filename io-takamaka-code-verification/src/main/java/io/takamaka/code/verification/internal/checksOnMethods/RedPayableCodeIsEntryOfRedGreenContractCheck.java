package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.RedPayableNotInRedGreenContractError;
import io.takamaka.code.verification.issues.RedPayableWithoutEntryError;

/**
 * A check that {@code @@RedPayable} is applied only to entries.
 */
public class RedPayableCodeIsEntryOfRedGreenContractCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public RedPayableCodeIsEntryOfRedGreenContractCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (annotations.isRedPayable(className, methodName, methodArgs, methodReturnType)) {
			if (!annotations.isEntry(className, methodName, methodArgs, methodReturnType))
				issue(new RedPayableWithoutEntryError(inferSourceFile(), methodName));

			if (!classLoader.isRedGreenContract(className))
				issue(new RedPayableNotInRedGreenContractError(inferSourceFile(), methodName));
		}
	}
}