package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class PayableWithoutAmountError extends Error {

	public PayableWithoutAmountError(ClassGen clazz, Method where) {
		super(clazz, where, "a @Payable method must have a first argument for the payed amount, of type int, long or BigInteger");
	}
}