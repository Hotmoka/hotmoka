package takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalPackageNameError extends Error {

	public IllegalPackageNameError(ClassGen clazz) {
		super(clazz, "package name is not allowed");
	}
}