package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalNativeMethodError extends Error {

	public IllegalNativeMethodError(ClassGen clazz, Method where) {
		super(clazz, where, "native code is not allowed");
	}
}