package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalSynchronizationError extends Error {

	public IllegalSynchronizationError(ClassGen clazz, Method where) {
		super(clazz, where, "synchronized methods are not allowed");
	}

	public IllegalSynchronizationError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "synchronization is not allowed");
	}
}