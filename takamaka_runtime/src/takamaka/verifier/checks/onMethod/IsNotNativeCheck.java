package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalNativeMethodError;

/**
 * A check the method is not synchronized.
 */
public class IsNotNativeCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public IsNotNativeCheck(VerifiedClassGen.Verification.MethodVerification verifier) {
		verifier.super();

		if (method.isNative())
			issue(new IllegalNativeMethodError(clazz, method));
	}
}