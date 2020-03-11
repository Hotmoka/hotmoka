package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalEntryArgumentError;
import io.takamaka.code.verification.issues.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class EntryCodeIsInstanceAndInContractsCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public EntryCodeIsInstanceAndInContractsCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		annotations.getEntryArgument(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.getContract().isAssignableFrom(tag))
				issue(new IllegalEntryArgumentError(inferSourceFile(), methodName));
			if (method.isStatic() || (!classLoader.isInterface(className) && !classLoader.isContract(className)))
				issue(new IllegalEntryMethodError(inferSourceFile(), methodName));
		});
	}
}