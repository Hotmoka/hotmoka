package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalEntryMethodError extends Error {

	public IllegalEntryMethodError(ClassGen clazz, Method where) {
		super(clazz, where, "@Entry can only be applied to constructors or instance methods of a contract");
	}
}