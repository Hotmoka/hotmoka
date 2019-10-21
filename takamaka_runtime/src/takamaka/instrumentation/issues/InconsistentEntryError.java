package takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class InconsistentEntryError extends Error {

	public InconsistentEntryError(ClassGen clazz, String methodName, String clazzWhereItWasDefined) {
		super(clazz, methodName, -1, "@Entry is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}