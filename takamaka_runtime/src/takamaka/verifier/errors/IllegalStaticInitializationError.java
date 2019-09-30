package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalStaticInitializationError extends Error {

	public IllegalStaticInitializationError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "illegal static initialization: only primitive or string final static fields bound to constants are allowed");
	}
}