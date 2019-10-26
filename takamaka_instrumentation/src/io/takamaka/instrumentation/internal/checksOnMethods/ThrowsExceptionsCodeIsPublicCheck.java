package io.takamaka.instrumentation.internal.checksOnMethods;

import io.takamaka.instrumentation.internal.VerifiedClass;
import io.takamaka.instrumentation.issues.ThrowsExceptionsOnNonPublicError;

/**
 * A checks that {@code @@ThrowsExceptions} methods are public.
 */
public class ThrowsExceptionsCodeIsPublicCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public ThrowsExceptionsCodeIsPublicCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		clazz.getMethodGens()
			.filter(method -> !method.isPublic() && clazz.annotations.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			.map(method -> new ThrowsExceptionsOnNonPublicError(clazz, method.getName()))
			.forEach(this::issue);
	}
}