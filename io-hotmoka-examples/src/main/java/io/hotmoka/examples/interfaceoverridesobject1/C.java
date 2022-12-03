package io.hotmoka.examples.interfaceoverridesobject1;

public class C implements MyInterface {

	public static int test() {
		// this calls Object.hashCode() and should be rejected
		return new C().hashCode();
	}
}
