package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalBootstrapMethodError extends Error {

	public IllegalBootstrapMethodError(ClassGen clazz) {
		super(clazz, "Illegal bootstrap method");
	}
}