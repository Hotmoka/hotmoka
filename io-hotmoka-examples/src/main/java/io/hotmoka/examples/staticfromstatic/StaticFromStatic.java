package io.hotmoka.examples.staticfromstatic;

public class StaticFromStatic {

	public static int foo() {
		return goo();
	}

	private static int goo() {
		return 42;
	}
}