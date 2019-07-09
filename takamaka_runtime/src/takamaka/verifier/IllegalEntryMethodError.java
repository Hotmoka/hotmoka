package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalEntryMethodError extends Error {

	public IllegalEntryMethodError(ClassGen clazz, Method where) {
		super(clazz, where, "@Entry can only be applied to public constructors or instance methods of a contract");
	}
}