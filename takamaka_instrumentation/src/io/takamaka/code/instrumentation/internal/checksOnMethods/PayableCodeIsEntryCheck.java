package io.takamaka.code.instrumentation.internal.checksOnMethods;

import io.takamaka.code.instrumentation.internal.VerifiedClass;
import io.takamaka.code.instrumentation.issues.PayableWithoutEntryError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class PayableCodeIsEntryCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public PayableCodeIsEntryCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (clazz.annotations.isPayable(className, methodName, methodArgs, methodReturnType)
				&& !clazz.annotations.isEntry(className, methodName, methodArgs, methodReturnType).isPresent())
			issue(new PayableWithoutEntryError(inferSourceFile(), methodName));
	}
}