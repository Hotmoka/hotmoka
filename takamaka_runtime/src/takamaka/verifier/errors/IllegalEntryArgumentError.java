package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalEntryArgumentError extends Error {

	public IllegalEntryArgumentError(ClassGen clazz, Method where) {
		super(clazz, where, "@Entry argument is not a contract");
	}
}