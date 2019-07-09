package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalEntryArgumentError extends Error {

	public IllegalEntryArgumentError(ClassGen clazz, Method where) {
		super(clazz, where, "@Entry argument is not a contract");
	}
}