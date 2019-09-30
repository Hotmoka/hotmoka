package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class CallerOutsideEntryError extends Error {

	public CallerOutsideEntryError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "caller() can only be used inside an @Entry method or constructor");
	}
}