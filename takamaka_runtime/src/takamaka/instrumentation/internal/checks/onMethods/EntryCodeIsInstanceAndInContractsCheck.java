package takamaka.instrumentation.internal.checks.onMethods;

import takamaka.instrumentation.internal.VerifiedClassGen;
import takamaka.instrumentation.issues.IllegalEntryArgumentError;
import takamaka.instrumentation.issues.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryCodeIsInstanceAndInContractsCheck extends VerifiedClassGen.ClassVerification.MethodVerification.Check {

	public EntryCodeIsInstanceAndInContractsCheck(VerifiedClassGen.ClassVerification.MethodVerification verification) {
		verification.super();

		classLoader.isEntry(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.contractClass.isAssignableFrom(tag))
				issue(new IllegalEntryArgumentError(clazz, methodName));
			if (method.isStatic() || !classLoader.isContract(className))
				issue(new IllegalEntryMethodError(clazz, methodName));
		});
	}
}