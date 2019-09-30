package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class InconsistentEntryError extends Error {

	public InconsistentEntryError(ClassGen clazz, Method where, String clazzWhereItWasDefined) {
		super(clazz, where, "@Entry is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}