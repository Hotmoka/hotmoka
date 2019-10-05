package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalEntryMethodError extends Error {

	public IllegalEntryMethodError(ClassGen clazz, String methodName) {
		super(clazz, methodName, -1, "@Entry can only be applied to constructors or instance methods of a contract");
	}
}