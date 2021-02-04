package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.ThrowsExceptionsOnNonPublicError;

/**
 * A checks that {@code @@ThrowsExceptions} methods are public.
 */
public class ThrowsExceptionsCodeIsPublicCheck extends CheckOnMethods {

	public ThrowsExceptionsCodeIsPublicCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (!method.isPublic() && annotations.isThrowsExceptions(className, method.getName(), method.getArgumentTypes(), method.getReturnType()))
			issue(new ThrowsExceptionsOnNonPublicError(inferSourceFile(), method.getName()));
	}
}