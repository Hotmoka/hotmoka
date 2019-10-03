package takamaka.verifier.checks;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.PayableWithoutAmountError;

/**
 * A checks that payable methods have an amount first argument.
 */
public class PayableMethodsReceiveAmountCheck extends VerifiedClassGen.ClassVerification.ClassLevelCheck {

	public PayableMethodsReceiveAmountCheck(VerifiedClassGen.ClassVerification verification) {
		verification.super();

		Stream.of(clazz.getMethods())
			.filter(method -> classLoader.isPayable(className, method.getName(), method.getArgumentTypes(), method.getReturnType()) && !startsWithAmount(method))
			.map(method -> new PayableWithoutAmountError(clazz, method))
			.forEach(this::issue);
	}

	private final static ObjectType BIG_INTEGER_OT = new ObjectType(BigInteger.class.getName());

	private static boolean startsWithAmount(Method method) {
		Type[] args = method.getArgumentTypes();
		return args.length > 0 && (args[0] == Type.INT || args[0] == Type.LONG || BIG_INTEGER_OT.equals(args[0]));
	}
}