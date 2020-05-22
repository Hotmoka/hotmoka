package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalFinalizerError;

/**
 * A check that the method is not a finalizer.
 */
public class IsNotFinalizerCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public IsNotFinalizerCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (!method.isPrivate() && "finalize".equals(method.getName()) && method.getReturnType() == Type.VOID && method.getArgumentTypes().length == 0)
			issue(new IllegalFinalizerError(inferSourceFile(), methodName));
	}
}