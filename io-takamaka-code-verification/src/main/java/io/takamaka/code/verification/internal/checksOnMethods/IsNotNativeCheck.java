package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalNativeMethodError;

/**
 * A check that the method is not native.
 */
public class IsNotNativeCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public IsNotNativeCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (method.isNative())
			issue(new IllegalNativeMethodError(inferSourceFile(), methodName));
	}
}