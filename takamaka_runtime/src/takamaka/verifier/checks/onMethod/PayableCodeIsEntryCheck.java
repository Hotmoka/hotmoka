package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.PayableWithoutEntryError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class PayableCodeIsEntryCheck extends VerifiedClassGen.Verifier.MethodVerifier.Check {

	public PayableCodeIsEntryCheck(VerifiedClassGen.Verifier.MethodVerifier verifier) {
		verifier.super();

		if (classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType())
				&& classLoader.isEntry(className, method.getName(), method.getArgumentTypes(), method.getReturnType()) == null)
			issue(new PayableWithoutEntryError(clazz, method));
	}
}