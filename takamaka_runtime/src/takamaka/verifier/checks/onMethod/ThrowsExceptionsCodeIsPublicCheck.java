package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.ThrowsExceptionsOnNonPublicError;

/**
 * A checks that {@code @@ThrowsExceptions} methods are public.
 */
public class ThrowsExceptionsCodeIsPublicCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public ThrowsExceptionsCodeIsPublicCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		clazz.getMethodGens()
			.filter(method -> !method.isPublic() && classLoader.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			.map(method -> new ThrowsExceptionsOnNonPublicError(clazz, method.getName()))
			.forEach(this::issue);
	}
}