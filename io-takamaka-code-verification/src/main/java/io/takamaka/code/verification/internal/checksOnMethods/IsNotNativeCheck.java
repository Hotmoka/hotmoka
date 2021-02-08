package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalNativeMethodError;

/**
 * A check that the method is not native.
 */
public class IsNotNativeCheck extends CheckOnMethods {

	public IsNotNativeCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (method.isNative())
			issue(new IllegalNativeMethodError(inferSourceFile(), methodName));
	}
}