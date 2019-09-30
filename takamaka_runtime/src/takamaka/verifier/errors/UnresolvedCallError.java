package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class UnresolvedCallError extends Error {

	public UnresolvedCallError(ClassGen clazz, Method where, int line, String declaringClassName, String methodName) {
		super(clazz, where, line, "unresolved call to " + declaringClassName + "." + methodName);
	}
}