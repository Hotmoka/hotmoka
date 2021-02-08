package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalEntryMethodError;
import io.takamaka.code.verification.issues.IllegalFromContractArgumentError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of storage classes.
 */
public class EntryCodeIsInstanceAndInStorageClassCheck extends CheckOnMethods {

	public EntryCodeIsInstanceAndInStorageClassCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		annotations.getFromContractArgument(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.getContract().isAssignableFrom(tag))
				issue(new IllegalFromContractArgumentError(inferSourceFile(), methodName));

			if (method.isStatic() || (!classLoader.isInterface(className) && !classLoader.isStorage(className)))
				issue(new IllegalEntryMethodError(inferSourceFile(), methodName));
		});
	}
}