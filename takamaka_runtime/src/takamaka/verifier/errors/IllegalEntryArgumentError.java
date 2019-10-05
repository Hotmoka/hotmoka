package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalEntryArgumentError extends Error {

	public IllegalEntryArgumentError(ClassGen clazz, String methodName) {
		super(clazz, methodName, -1, "@Entry argument is not a contract");
	}
}