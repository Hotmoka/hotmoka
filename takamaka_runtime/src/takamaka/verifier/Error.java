package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

/**
 * A blocking issue: if an error occurs during the processing of a Takamaka jar file,
 * then its instrumentation cannot proceed and will be aborted.
 */
public abstract class Error extends Issue {

	protected Error(String where, String message) {
		super(where, message);
	}

	protected Error(ClassGen where, String message) {
		super(where, message);
	}

	protected Error(ClassGen clazz, Method where, String message) {
		super(clazz, where, message);
	}

	protected Error(ClassGen clazz, Method where, int line, String message) {
		super(clazz, where, line, message);
	}
}