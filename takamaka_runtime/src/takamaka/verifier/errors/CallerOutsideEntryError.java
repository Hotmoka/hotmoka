package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class CallerOutsideEntryError extends Error {

	public CallerOutsideEntryError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "caller() can only be used inside an @Entry method or constructor");
	}
}