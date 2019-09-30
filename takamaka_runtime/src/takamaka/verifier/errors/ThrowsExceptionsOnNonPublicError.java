package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class ThrowsExceptionsOnNonPublicError extends Error {

	public ThrowsExceptionsOnNonPublicError(ClassGen clazz, Method where) {
		super(clazz, where, "@ThrowsExceptions can only be applied to a public constructor or method");
	}
}