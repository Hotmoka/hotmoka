package takamaka.instrumentation.internal.checks.onMethods;

import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.IllegalSynchronizationError;

/**
 * A check the method is not synchronized.
 */
public class IsNotSynchronizedCheck extends VerifiedClassGen.ClassVerification.MethodVerification.Check {

	public IsNotSynchronizedCheck(VerifiedClassGen.ClassVerification.MethodVerification verification) {
		verification.super();

		if (method.isSynchronized())
			issue(new IllegalSynchronizationError(clazz, methodName));
	}
}