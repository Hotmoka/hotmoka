package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class PayableWithoutEntryError extends Error {

	public PayableWithoutEntryError(ClassGen clazz, Method where) {
		super(clazz, where, "@Payable can only be applied to an @Entry method or constructor");
	}
}