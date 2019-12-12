package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.RedPayableWithoutEntryError;

/**
 * A check that {@code @@RedPayable} is applied only to entries.
 */
public class RedPayableCodeIsEntryCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public RedPayableCodeIsEntryCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (annotations.isRedPayable(className, methodName, methodArgs, methodReturnType)
				&& !annotations.isEntry(className, methodName, methodArgs, methodReturnType).isPresent())
			issue(new RedPayableWithoutEntryError(inferSourceFile(), methodName));
	}
}