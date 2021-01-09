package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalFromContractArgumentError;
import io.takamaka.code.verification.issues.IllegalEntryMethodError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of storage classes.
 */
public class EntryCodeIsInstanceAndInStorageClassCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public EntryCodeIsInstanceAndInStorageClassCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		annotations.getFromContractArgument(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.getContract().isAssignableFrom(tag))
				issue(new IllegalFromContractArgumentError(inferSourceFile(), methodName));

			if (method.isStatic() || (!classLoader.isInterface(className) && !classLoader.isStorage(className)))
				issue(new IllegalEntryMethodError(inferSourceFile(), methodName));
		});
	}
}