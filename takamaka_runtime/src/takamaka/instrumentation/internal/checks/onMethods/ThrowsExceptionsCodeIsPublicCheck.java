package takamaka.instrumentation.internal.checks.onMethods;

import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.ThrowsExceptionsOnNonPublicError;

/**
 * A checks that {@code @@ThrowsExceptions} methods are public.
 */
public class ThrowsExceptionsCodeIsPublicCheck extends VerifiedClassGen.ClassVerification.MethodVerification.Check {

	public ThrowsExceptionsCodeIsPublicCheck(VerifiedClassGen.ClassVerification.MethodVerification verification) {
		verification.super();

		clazz.getMethodGens()
			.filter(method -> !method.isPublic() && classLoader.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			.map(method -> new ThrowsExceptionsOnNonPublicError(clazz, method.getName()))
			.forEach(this::issue);
	}
}