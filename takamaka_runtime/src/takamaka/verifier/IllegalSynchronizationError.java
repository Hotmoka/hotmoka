package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalSynchronizationError extends Error {

	public IllegalSynchronizationError(ClassGen clazz, Method where) {
		super(clazz, where, "synchronized methods are not allowed");
	}

	public IllegalSynchronizationError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "synchronization is not allowed");
	}
}