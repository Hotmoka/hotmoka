package takamaka.verifier.internal.checks.onMethods;

import takamaka.verifier.errors.IllegalEntryArgumentError;
import takamaka.verifier.errors.IllegalEntryMethodError;
import takamaka.verifier.internal.VerifiedClassGenImpl;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryCodeIsInstanceAndInContractsCheck extends VerifiedClassGenImpl.Verification.MethodVerification.Check {

	public EntryCodeIsInstanceAndInContractsCheck(VerifiedClassGenImpl.Verification.MethodVerification verification) {
		verification.super();

		classLoader.isEntry(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.contractClass.isAssignableFrom(tag))
				issue(new IllegalEntryArgumentError(clazz, methodName));
			if (method.isStatic() || !classLoader.isContract(className))
				issue(new IllegalEntryMethodError(clazz, methodName));
		});
	}
}