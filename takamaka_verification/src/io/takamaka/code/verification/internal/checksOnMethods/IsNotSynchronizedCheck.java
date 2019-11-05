package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalSynchronizationError;

/**
 * A check the method is not synchronized.
 */
public class IsNotSynchronizedCheck extends VerifiedClassImpl.ClassVerification.MethodVerification.Check {

	public IsNotSynchronizedCheck(VerifiedClassImpl.ClassVerification.MethodVerification verification) {
		verification.super();

		if (method.isSynchronized())
			issue(new IllegalSynchronizationError(inferSourceFile(), methodName));
	}
}