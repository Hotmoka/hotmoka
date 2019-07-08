package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class PayableWithoutEntryError extends Error {

	public PayableWithoutEntryError(ClassGen clazz, Method where) {
		super(clazz, where, "@Payable can only be applied to an @Entry method or constructor");
	}
}