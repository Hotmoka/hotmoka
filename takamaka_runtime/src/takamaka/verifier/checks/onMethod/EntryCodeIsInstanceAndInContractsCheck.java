package takamaka.verifier.checks.onMethod;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalEntryArgumentError;
import takamaka.verifier.errors.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryCodeIsInstanceAndInContractsCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public EntryCodeIsInstanceAndInContractsCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		classLoader.isEntry(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.contractClass.isAssignableFrom(tag))
				issue(new IllegalEntryArgumentError(clazz, methodName));
			if (method.isStatic() || !classLoader.isContract(className))
				issue(new IllegalEntryMethodError(clazz, methodName));
		});
	}
}