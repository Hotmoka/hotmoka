package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalCallToEntryError extends Error {

	public IllegalCallToEntryError(ClassGen clazz, String methodName, String entryName, int line) {
		super(clazz, methodName, line, "\"" + entryName + "\" is an @Entry, hence can only be called from an instance method of a contract");
	}
}