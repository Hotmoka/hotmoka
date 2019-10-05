package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalSynchronizationError;

/**
 * A check the method is not synchronized.
 */
public class IsNotSynchronizedCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public IsNotSynchronizedCheck(VerifiedClassGen.Verification.MethodVerification verifier) {
		verifier.super();

		if (method.isSynchronized())
			issue(new IllegalSynchronizationError(clazz, method.getName()));
	}
}