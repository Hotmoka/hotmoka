package takamaka.verifier.internal.checks.onMethods;

import takamaka.verifier.errors.IllegalNativeMethodError;
import takamaka.verifier.internal.VerifiedClassGenImpl;

/**
 * A check the method is not synchronized.
 */
public class IsNotNativeCheck extends VerifiedClassGenImpl.Verification.MethodVerification.Check {

	public IsNotNativeCheck(VerifiedClassGenImpl.Verification.MethodVerification verification) {
		verification.super();

		if (method.isNative())
			issue(new IllegalNativeMethodError(clazz, methodName));
	}
}