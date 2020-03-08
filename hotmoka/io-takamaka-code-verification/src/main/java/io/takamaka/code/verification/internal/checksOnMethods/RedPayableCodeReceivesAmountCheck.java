package io.takamaka.code.verification.internal.checksOnMethods;

import java.math.BigInteger;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.RedPayableWithoutAmountError;

/**
 * A checks that red payable methods have an amount first argument.
 */
public class RedPayableCodeReceivesAmountCheck extends VerifiedClassImpl.Builder.MethodVerification.Check {

	public RedPayableCodeReceivesAmountCheck(VerifiedClassImpl.Builder.MethodVerification verification) {
		verification.super();

		if (annotations.isRedPayable(className, methodName, methodArgs, methodReturnType) && !startsWithAmount())
			issue(new RedPayableWithoutAmountError(inferSourceFile(), methodName));
	}

	private final static ObjectType BIG_INTEGER_OT = new ObjectType(BigInteger.class.getName());

	private boolean startsWithAmount() {
		return methodArgs.length > 0 && (methodArgs[0] == Type.INT || methodArgs[0] == Type.LONG || BIG_INTEGER_OT.equals(methodArgs[0]));
	}
}