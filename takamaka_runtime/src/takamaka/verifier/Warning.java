package takamaka.verifier;

import org.apache.bcel.generic.ClassGen;

/**
 * A non-blocking issue: if a warning occurs during the processing of a Takamaka jar file,
 * then its instrumentation does proceed and will not be aborted.
 */
public abstract class Warning extends Issue {

	protected Warning(ClassGen where, String message) {
		super(where, message);
	}

	protected Warning(ClassGen clazz, String methodName, int line, String message) {
		super(clazz, methodName, line, message);
	}

	protected Warning(ClassGen clazz, String fieldName, String message) {
		super(clazz, fieldName, message);
	}
}