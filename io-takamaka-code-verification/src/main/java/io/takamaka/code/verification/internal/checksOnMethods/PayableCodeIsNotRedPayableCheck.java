package io.takamaka.code.verification.internal.checksOnMethods;

import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.PayableWithRedPayableError;

/**
 * A check that {@code @@Payable} and {@code @@RedPayable} are not applied together to the same
 * method or constructor.
 */
public class PayableCodeIsNotRedPayableCheck extends CheckOnMethods {

	public PayableCodeIsNotRedPayableCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (annotations.isPayable(className, methodName, methodArgs, methodReturnType)
				&& annotations.isRedPayable(className, methodName, methodArgs, methodReturnType))
			issue(new PayableWithRedPayableError(inferSourceFile(), methodName));
	}
}