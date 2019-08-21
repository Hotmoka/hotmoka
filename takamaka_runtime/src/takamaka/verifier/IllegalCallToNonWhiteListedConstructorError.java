package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalCallToNonWhiteListedConstructorError extends Error {

	public IllegalCallToNonWhiteListedConstructorError(ClassGen clazz, Method where, int line, String declaringClassName) {
		super(clazz, where, line, "illegal call to non-white-listed constructor of " + declaringClassName);
	}
}