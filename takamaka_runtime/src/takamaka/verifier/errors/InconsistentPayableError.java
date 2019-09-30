package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class InconsistentPayableError extends Error {

	public InconsistentPayableError(ClassGen clazz, Method where, String clazzWhereItWasDefined) {
		super(clazz, where, "@Payable is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}