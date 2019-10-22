package takamaka.instrumentation.internal.checksOnMethods;

import java.math.BigInteger;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import takamaka.instrumentation.internal.VerifiedClass;
import takamaka.instrumentation.issues.PayableWithoutAmountError;

/**
 * A checks that payable methods have an amount first argument.
 */
public class PayableCodeReceivesAmountCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public PayableCodeReceivesAmountCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (clazz.annotations.isPayable(className, methodName, methodArgs, methodReturnType) && !startsWithAmount())
			issue(new PayableWithoutAmountError(clazz, methodName));
	}

	private final static ObjectType BIG_INTEGER_OT = new ObjectType(BigInteger.class.getName());

	private boolean startsWithAmount() {
		return methodArgs.length > 0 && (methodArgs[0] == Type.INT || methodArgs[0] == Type.LONG || BIG_INTEGER_OT.equals(methodArgs[0]));
	}
}