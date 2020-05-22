package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalSynchronizationError;

/**
 * A check that the method is not synchronized.
 */
public class IsNotSynchronizedCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public IsNotSynchronizedCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (method.isSynchronized())
			issue(new IllegalSynchronizationError(inferSourceFile(), methodName));
	}
}