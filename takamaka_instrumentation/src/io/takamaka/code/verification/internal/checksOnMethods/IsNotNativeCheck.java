package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.VerifiedClass;
import io.takamaka.code.verification.issues.IllegalNativeMethodError;

/**
 * A check the method is not synchronized.
 */
public class IsNotNativeCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public IsNotNativeCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (method.isNative())
			issue(new IllegalNativeMethodError(inferSourceFile(), methodName));
	}
}