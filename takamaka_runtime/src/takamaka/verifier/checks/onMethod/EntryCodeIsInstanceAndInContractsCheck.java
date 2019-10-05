package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalEntryArgumentError;
import takamaka.verifier.errors.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryCodeIsInstanceAndInContractsCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public EntryCodeIsInstanceAndInContractsCheck(VerifiedClassGen.Verification.MethodVerification verifier) {
		verifier.super();

		boolean isContract = classLoader.isContract(className);
		String methodName = method.getName();
		Class<?> isEntry = classLoader.isEntry(className, methodName, method.getArgumentTypes(), method.getReturnType());

		if (isEntry != null) {
			if (!classLoader.contractClass.isAssignableFrom(isEntry))
				issue(new IllegalEntryArgumentError(clazz, methodName));
			if (method.isStatic() || !isContract)
				issue(new IllegalEntryMethodError(clazz, methodName));
		}
	}
}