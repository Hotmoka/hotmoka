package takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalNativeMethodError extends Error {

	public IllegalNativeMethodError(ClassGen clazz, String methodName) {
		super(clazz, methodName, -1, "native code is not allowed");
	}
}