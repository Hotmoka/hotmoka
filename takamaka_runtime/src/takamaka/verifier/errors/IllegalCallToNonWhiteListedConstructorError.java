package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalCallToNonWhiteListedConstructorError extends Error {

	public IllegalCallToNonWhiteListedConstructorError(ClassGen clazz, Method where, int line, String declaringClassName) {
		super(clazz, where, line, "illegal call to non-white-listed constructor of " + declaringClassName);
	}
}