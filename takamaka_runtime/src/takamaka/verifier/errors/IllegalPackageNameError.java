package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalPackageNameError extends Error {

	public IllegalPackageNameError(ClassGen clazz) {
		super(clazz, "package name is not allowed");
	}
}