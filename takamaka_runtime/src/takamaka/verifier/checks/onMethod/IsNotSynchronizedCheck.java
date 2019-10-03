package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalSynchronizationError;

/**
 * A check the method is not synchronized.
 */
public class IsNotSynchronizedCheck extends VerifiedClassGen.Verifier.MethodVerifier.Check {

	public IsNotSynchronizedCheck(VerifiedClassGen.Verifier.MethodVerifier verifier) {
		verifier.super();

		if (method.isSynchronized())
			issue(new IllegalSynchronizationError(clazz, method));
	}
}