package takamaka.verifier.checks.onMethod;

import java.util.stream.Stream;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.ThrowsExceptionsOnNonPublicError;

/**
 * A checks that {@code @@ThrowsExceptions} methods are public.
 */
public class ThrowsExceptionsCodeIsPublicCheck extends VerifiedClassGen.Verifier.MethodVerifier.Check {

	public ThrowsExceptionsCodeIsPublicCheck(VerifiedClassGen.Verifier.MethodVerifier verifier) {
		verifier.super();

		Stream.of(clazz.getMethods())
			.filter(method -> !method.isPublic() && classLoader.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			.map(method -> new ThrowsExceptionsOnNonPublicError(clazz, method))
			.forEach(this::issue);
	}
}