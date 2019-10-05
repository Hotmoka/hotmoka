package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalCallToNonWhiteListedMethodError extends Error {

	public IllegalCallToNonWhiteListedMethodError(ClassGen clazz, String methodName, int line, String declaringClassName, String calledMethodName) {
		super(clazz, methodName, line, "illegal call to non-white-listed method " + declaringClassName + "." + calledMethodName);
	}
}