package io.takamaka.instrumentation.internal.checksOnMethods;

import io.takamaka.instrumentation.internal.VerifiedClass;
import io.takamaka.instrumentation.issues.IllegalSynchronizationError;

/**
 * A check the method is not synchronized.
 */
public class IsNotSynchronizedCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public IsNotSynchronizedCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (method.isSynchronized())
			issue(new IllegalSynchronizationError(clazz, methodName));
	}
}