package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalNativeMethodError;

/**
 * A check the method is not synchronized.
 */
public class IsNotNativeCheck extends VerifiedClassGen.Verifier.MethodVerifier.Check {

	public IsNotNativeCheck(VerifiedClassGen.Verifier.MethodVerifier verifier) {
		verifier.super();

		if (method.isNative())
			issue(new IllegalNativeMethodError(clazz, method));
	}
}