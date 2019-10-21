package takamaka.verifier.internal.checks.onMethods;

import takamaka.verifier.errors.IllegalSynchronizationError;
import takamaka.verifier.internal.VerifiedClassGenImpl;

/**
 * A check the method is not synchronized.
 */
public class IsNotSynchronizedCheck extends VerifiedClassGenImpl.Verification.MethodVerification.Check {

	public IsNotSynchronizedCheck(VerifiedClassGenImpl.Verification.MethodVerification verification) {
		verification.super();

		if (method.isSynchronized())
			issue(new IllegalSynchronizationError(clazz, methodName));
	}
}