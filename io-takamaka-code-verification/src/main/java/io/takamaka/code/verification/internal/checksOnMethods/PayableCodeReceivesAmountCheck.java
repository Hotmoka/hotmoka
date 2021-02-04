package io.takamaka.code.verification.internal.checksOnMethods;

import java.math.BigInteger;

import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.PayableWithoutAmountError;

/**
 * A checks that payable methods have an amount first argument.
 */
public class PayableCodeReceivesAmountCheck extends CheckOnMethods {

	public PayableCodeReceivesAmountCheck(VerifiedClassImpl.Builder builder, MethodGen method) {
		super(builder, method);

		if (annotations.isPayable(className, methodName, methodArgs, methodReturnType) && !startsWithAmount())
			issue(new PayableWithoutAmountError(inferSourceFile(), methodName));
	}

	private final static ObjectType BIG_INTEGER_OT = new ObjectType(BigInteger.class.getName());

	private boolean startsWithAmount() {
		// for constructors of instance inner classes, we must skip the instrumented extra parameter holding the parent object
		int amountArgPos = isConstructorOfInnerNonStaticClass ? 1 : 0;

		return methodArgs.length > amountArgPos &&
			(methodArgs[amountArgPos] == Type.INT || methodArgs[amountArgPos] == Type.LONG || BIG_INTEGER_OT.equals(methodArgs[amountArgPos]));
	}
}