package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalNativeMethodError extends Error {

	public IllegalNativeMethodError(ClassGen clazz, Method where) {
		super(clazz, where, "native code is not allowed");
	}
}