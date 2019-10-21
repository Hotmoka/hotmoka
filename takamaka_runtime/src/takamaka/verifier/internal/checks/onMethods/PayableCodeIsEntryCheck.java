package takamaka.verifier.internal.checks.onMethods;

import takamaka.verifier.errors.PayableWithoutEntryError;
import takamaka.verifier.internal.VerifiedClassGenImpl;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class PayableCodeIsEntryCheck extends VerifiedClassGenImpl.Verification.MethodVerification.Check {

	public PayableCodeIsEntryCheck(VerifiedClassGenImpl.Verification.MethodVerification verification) {
		verification.super();

		if (classLoader.isPayable(className, methodName, methodArgs, methodReturnType)
				&& !classLoader.isEntry(className, methodName, methodArgs, methodReturnType).isPresent())
			issue(new PayableWithoutEntryError(clazz, methodName));
	}
}