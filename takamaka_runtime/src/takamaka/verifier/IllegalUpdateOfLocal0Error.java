package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalUpdateOfLocal0Error extends Error {

	public IllegalUpdateOfLocal0Error(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "local 0 (\"this\") cannot be modified");
	}
}