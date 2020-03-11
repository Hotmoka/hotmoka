package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.RedPayableInSimpleContractError;

/**
 * A check that {@code @@RedPayable} is applied only to constructors or methods of red/green contracts.
 */
public class RedPayableCodeIsInRedGreenContract extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public RedPayableCodeIsInRedGreenContract(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (annotations.isRedPayable(className, methodName, methodArgs, methodReturnType) && !classLoader.isRedGreenContract(className) && !classLoader.isInterface(className))
			issue(new RedPayableInSimpleContractError(inferSourceFile(), methodName));
	}
}