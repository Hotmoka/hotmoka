package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalSynchronizationError;

/**
 * A check that the method is not synchronized.
 */
public class IsNotSynchronizedCheck extends CheckOnMethods {

	public IsNotSynchronizedCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (method.isSynchronized())
			issue(new IllegalSynchronizationError(inferSourceFile(), methodName));
	}
}