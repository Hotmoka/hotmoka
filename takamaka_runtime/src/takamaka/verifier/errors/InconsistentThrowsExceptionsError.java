package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class InconsistentThrowsExceptionsError extends Error {

	public InconsistentThrowsExceptionsError(ClassGen clazz, String methodName, String clazzWhereItWasDefined) {
		super(clazz, methodName, -1, "@ThrowsExceptions is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}