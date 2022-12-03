package io.hotmoka.examples.interfaceoverridesobject2;

public class C implements MyInterface {
	public int foo(MyInterface c) {
		// this calls Object.hashCode() and should be rejected
		return c.hashCode();
	}
	
	public static int test() {
		C c = new C();
		return c.foo(c);
	}
}
