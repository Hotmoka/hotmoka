package takamaka.instrumentation.internal.checksOnMethods;

import takamaka.instrumentation.internal.VerifiedClass;
import takamaka.instrumentation.issues.IllegalNativeMethodError;

/**
 * A check the method is not synchronized.
 */
public class IsNotNativeCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public IsNotNativeCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (method.isNative())
			issue(new IllegalNativeMethodError(clazz, methodName));
	}
}