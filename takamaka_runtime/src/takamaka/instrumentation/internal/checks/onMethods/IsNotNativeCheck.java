package takamaka.instrumentation.internal.checks.onMethods;

import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.IllegalNativeMethodError;

/**
 * A check the method is not synchronized.
 */
public class IsNotNativeCheck extends VerifiedClassGen.ClassVerification.MethodVerification.Check {

	public IsNotNativeCheck(VerifiedClassGen.ClassVerification.MethodVerification verification) {
		verification.super();

		if (method.isNative())
			issue(new IllegalNativeMethodError(clazz, methodName));
	}
}