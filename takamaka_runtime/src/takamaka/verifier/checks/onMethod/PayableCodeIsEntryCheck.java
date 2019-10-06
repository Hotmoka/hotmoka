package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.PayableWithoutEntryError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class PayableCodeIsEntryCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public PayableCodeIsEntryCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		if (classLoader.isPayable(className, methodName, methodArgs, methodReturnType)
				&& !classLoader.isEntry(className, methodName, methodArgs, methodReturnType).isPresent())
			issue(new PayableWithoutEntryError(clazz, methodName));
	}
}