package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class InconsistentThrowsExceptionsError extends Error {

	public InconsistentThrowsExceptionsError(ClassGen clazz, Method where, String clazzWhereItWasDefined) {
		super(clazz, where, "@ThrowsExceptions is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}