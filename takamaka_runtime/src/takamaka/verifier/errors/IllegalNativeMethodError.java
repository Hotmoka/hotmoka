package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalNativeMethodError extends Error {

	public IllegalNativeMethodError(ClassGen clazz, String methodName) {
		super(clazz, methodName, -1, "native code is not allowed");
	}
}