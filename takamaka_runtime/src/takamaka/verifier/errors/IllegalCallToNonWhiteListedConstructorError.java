package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalCallToNonWhiteListedConstructorError extends Error {

	public IllegalCallToNonWhiteListedConstructorError(ClassGen clazz, String methodName, int line, String declaringClassName) {
		super(clazz, methodName, line, "illegal call to non-white-listed constructor of " + declaringClassName);
	}
}