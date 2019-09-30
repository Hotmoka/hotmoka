package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalUpdateOfLocal0Error extends Error {

	public IllegalUpdateOfLocal0Error(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "local 0 (\"this\") cannot be modified");
	}
}