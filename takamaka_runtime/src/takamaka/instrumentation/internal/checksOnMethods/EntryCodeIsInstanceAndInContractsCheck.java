package takamaka.instrumentation.internal.checksOnMethods;

import takamaka.instrumentation.internal.VerifiedClass;
import takamaka.instrumentation.issues.IllegalEntryArgumentError;
import takamaka.instrumentation.issues.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryCodeIsInstanceAndInContractsCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public EntryCodeIsInstanceAndInContractsCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		clazz.annotations.isEntry(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.contractClass.isAssignableFrom(tag))
				issue(new IllegalEntryArgumentError(clazz, methodName));
			if (method.isStatic() || !classLoader.isContract(className))
				issue(new IllegalEntryMethodError(clazz, methodName));
		});
	}
}