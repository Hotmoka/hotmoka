package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalNativeMethodError;

/**
 * A check the method is not synchronized.
 */
public class IsNotNativeCheck extends VerifiedClassImpl.ClassVerification.MethodVerification.Check {

	public IsNotNativeCheck(VerifiedClassImpl.ClassVerification.MethodVerification verification) {
		verification.super();

		if (method.isNative())
			issue(new IllegalNativeMethodError(inferSourceFile(), methodName));
	}
}