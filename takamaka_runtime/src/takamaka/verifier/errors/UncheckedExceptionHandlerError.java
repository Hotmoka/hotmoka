package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class UncheckedExceptionHandlerError extends Error {

	public UncheckedExceptionHandlerError(ClassGen clazz, Method where, int line, String exceptionName) {
		super(clazz, where, line, "exception handler for unchecked exception " + exceptionName);
	}
}