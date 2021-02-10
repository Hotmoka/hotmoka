package io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod5;

public class C {

	public static String foo() {
		return test(new Object(), new Object());
	}

	private static String test(Object arg1, Object arg2) {
		return "" + arg1 + arg2; // KO at run time, since it calls toString() on Object
	}
}