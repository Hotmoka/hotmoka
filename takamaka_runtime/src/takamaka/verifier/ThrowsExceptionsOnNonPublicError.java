package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class ThrowsExceptionsOnNonPublicError extends Error {

	public ThrowsExceptionsOnNonPublicError(ClassGen clazz, Method where) {
		super(clazz, where, "@ThrowsExceptions can only be applied to a public constructor or method");
	}
}