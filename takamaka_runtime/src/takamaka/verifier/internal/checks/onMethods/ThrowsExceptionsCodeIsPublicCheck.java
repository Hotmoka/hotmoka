package takamaka.verifier.internal.checks.onMethods;

import takamaka.verifier.errors.ThrowsExceptionsOnNonPublicError;
import takamaka.verifier.internal.VerifiedClassGenImpl;

/**
 * A checks that {@code @@ThrowsExceptions} methods are public.
 */
public class ThrowsExceptionsCodeIsPublicCheck extends VerifiedClassGenImpl.Verification.MethodVerification.Check {

	public ThrowsExceptionsCodeIsPublicCheck(VerifiedClassGenImpl.Verification.MethodVerification verification) {
		verification.super();

		clazz.getMethodGens()
			.filter(method -> !method.isPublic() && classLoader.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			.map(method -> new ThrowsExceptionsOnNonPublicError(clazz, method.getName()))
			.forEach(this::issue);
	}
}