package takamaka.verifier.checks.onMethod;

import java.math.BigInteger;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.PayableWithoutAmountError;

/**
 * A checks that payable methods have an amount first argument.
 */
public class PayableCodeReceivesAmountCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public PayableCodeReceivesAmountCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		String methodName = method.getName();
		if (classLoader.isPayable(className, methodName, method.getArgumentTypes(), method.getReturnType()) && !startsWithAmount(method))
			issue(new PayableWithoutAmountError(clazz, methodName));
	}

	private final static ObjectType BIG_INTEGER_OT = new ObjectType(BigInteger.class.getName());

	private static boolean startsWithAmount(Method method) {
		Type[] args = method.getArgumentTypes();
		return args.length > 0 && (args[0] == Type.INT || args[0] == Type.LONG || BIG_INTEGER_OT.equals(args[0]));
	}
}