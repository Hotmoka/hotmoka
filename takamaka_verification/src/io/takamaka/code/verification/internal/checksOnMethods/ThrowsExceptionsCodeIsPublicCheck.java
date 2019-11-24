package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.ThrowsExceptionsOnNonPublicError;

/**
 * A checks that {@code @@ThrowsExceptions} methods are public.
 */
public class ThrowsExceptionsCodeIsPublicCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public ThrowsExceptionsCodeIsPublicCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		getMethodGens()
			.filter(method -> !method.isPublic() && annotations.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			.map(method -> new ThrowsExceptionsOnNonPublicError(inferSourceFile(), method.getName()))
			.forEachOrdered(this::issue);
	}
}