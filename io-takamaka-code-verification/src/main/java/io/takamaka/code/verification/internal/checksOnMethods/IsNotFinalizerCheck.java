package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalFinalizerError;

/**
 * A check that the method is not a finalizer.
 */
public class IsNotFinalizerCheck extends CheckOnMethods {

	public IsNotFinalizerCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (!method.isPrivate() && "finalize".equals(method.getName()) && method.getReturnType() == Type.VOID && method.getArgumentTypes().length == 0)
			issue(new IllegalFinalizerError(inferSourceFile(), methodName));
	}
}