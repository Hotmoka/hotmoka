package takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalEntryArgumentError extends Error {

	public IllegalEntryArgumentError(ClassGen clazz, String methodName) {
		super(clazz, methodName, -1, "@Entry argument is not a contract");
	}
}