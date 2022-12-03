package io.hotmoka.examples.interfaceoverridesobject2;

public interface MyInterface {
	// the goal is to confuse the verifier into believing
	// that this method is safe since it is defined in the code in blockchain
	public int hashCode();
}
