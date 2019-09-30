package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalCallToNonWhiteListedMethodError extends Error {

	public IllegalCallToNonWhiteListedMethodError(ClassGen clazz, Method where, int line, String declaringClassName, String methodName) {
		super(clazz, where, line, "illegal call to non-white-listed method " + declaringClassName + "." + methodName);
	}
}