package takamaka.instrumentation.internal.checks.onMethods;

import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.PayableWithoutEntryError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class PayableCodeIsEntryCheck extends VerifiedClassGen.ClassVerification.MethodVerification.Check {

	public PayableCodeIsEntryCheck(VerifiedClassGen.ClassVerification.MethodVerification verification) {
		verification.super();

		if (classLoader.isPayable(className, methodName, methodArgs, methodReturnType)
				&& !classLoader.isEntry(className, methodName, methodArgs, methodReturnType).isPresent())
			issue(new PayableWithoutEntryError(clazz, methodName));
	}
}