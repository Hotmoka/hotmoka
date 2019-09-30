package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalCallToEntryError extends Error {

	public IllegalCallToEntryError(ClassGen clazz, Method where, String entryName, int line) {
		super(clazz, where, line, "\"" + entryName + "\" is an @Entry, hence can only be called from an instance method of a contract");
	}
}