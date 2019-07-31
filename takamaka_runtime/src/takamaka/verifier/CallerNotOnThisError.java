package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class CallerNotOnThisError extends Error {

	public CallerNotOnThisError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "caller() can only be called on \"this\"");
	}
}