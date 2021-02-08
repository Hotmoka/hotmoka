package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.PayableNotInContractError;
import io.takamaka.code.verification.issues.PayableWithoutFromContractError;

/**
 * A check that {@code @@Payable} is applied only to from contract code of contracts.
 */
public class PayableCodeIsFromContractCheck extends CheckOnMethods {

	public PayableCodeIsFromContractCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (annotations.isPayable(className, methodName, methodArgs, methodReturnType)) {
			if (!annotations.isFromContract(className, methodName, methodArgs, methodReturnType))
				issue(new PayableWithoutFromContractError(inferSourceFile(), methodName));

			if (!classLoader.isContract(className) && !classLoader.isInterface(className))
				issue(new PayableNotInContractError(inferSourceFile(), methodName));
		}
	}
}