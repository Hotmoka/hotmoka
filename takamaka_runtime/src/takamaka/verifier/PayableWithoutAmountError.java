package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class PayableWithoutAmountError extends Error {

	public PayableWithoutAmountError(ClassGen clazz, Method where) {
		super(clazz, where, "this @Payable method must have a first argument of type int, long or BigInteger");
	}
}