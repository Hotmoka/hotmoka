package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.FromContractNotInStorageError;
import io.takamaka.code.verification.issues.IllegalFromContractArgumentError;

/**
 * A check that {@code @@FromContract} is applied only to instance methods or constructors of storage classes or interfaces.
 */
public class FromContractCodeIsInstanceAndInStorageClassCheck extends CheckOnMethods {

	public FromContractCodeIsInstanceAndInStorageClassCheck(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

		annotations.getFromContractArgument(className, methodName, methodArgs, methodReturnType).ifPresent(tag -> {
			if (!classLoader.getContract().isAssignableFrom(tag))
				issue(new IllegalFromContractArgumentError(inferSourceFile(), methodName));

			if (method.isStatic() || (!classLoader.isInterface(className) && !classLoader.isStorage(className)))
				issue(new FromContractNotInStorageError(inferSourceFile(), methodName));
		});
	}
}