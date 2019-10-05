package takamaka.verifier;

import org.apache.bcel.generic.ClassGen;

/**
 * A blocking issue: if an error occurs during the processing of a Takamaka jar file,
 * then its instrumentation cannot proceed and will be aborted.
 */
public abstract class Error extends Issue {

	protected Error(ClassGen where, String message) {
		super(where, message);
	}

	protected Error(ClassGen clazz, String fieldName, String message) {
		super(clazz, fieldName, message);
	}

	protected Error(ClassGen clazz, String methodName, int line, String message) {
		super(clazz, methodName, line, message);
	}
}