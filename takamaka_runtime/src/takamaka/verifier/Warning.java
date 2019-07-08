package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

/**
 * A non-blocking issue: if a warning occurs during the processing of a Takamaka jar file,
 * then its instrumentation does proceed and will not be aborted.
 */
public abstract class Warning extends Issue {

	protected Warning(String where, String message) {
		super(where, message);
	}

	protected Warning(ClassGen where, String message) {
		super(where, message);
	}

	protected Warning(ClassGen clazz, Method where, String message) {
		super(clazz, where, message);
	}
}