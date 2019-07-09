package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class CallerNotOnThis extends Error {

	public CallerNotOnThis(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "caller() can only be called on \"this\"");
	}
}