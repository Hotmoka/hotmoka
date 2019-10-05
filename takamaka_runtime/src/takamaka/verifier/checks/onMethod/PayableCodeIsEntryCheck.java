package takamaka.verifier.checks.onMethod;

import org.apache.bcel.generic.Type;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.PayableWithoutEntryError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class PayableCodeIsEntryCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public PayableCodeIsEntryCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		String methodName = method.getName();
		Type[] args = method.getArgumentTypes();
		Type returnType = method.getReturnType();
		if (classLoader.isPayable(className, methodName, args, returnType) && classLoader.isEntry(className, methodName, args, returnType) == null)
			issue(new PayableWithoutEntryError(clazz, methodName));
	}
}