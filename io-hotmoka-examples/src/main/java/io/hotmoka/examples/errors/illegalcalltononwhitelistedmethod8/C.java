package io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod8;

public class C {

	public static String foo() {
		Object o1 = new Object();
		Object o2 = new Object();
		test1(o1, o2);
		return test2(o1, o2);
	}

	private static String test1(Object arg1, Object arg2) {
		return "" + arg1 + arg2; // KO at run time, since it calls toString() on Object
	}

	private static String test2(Object arg1, Object arg2) {
		return "" + arg1 + arg2; // KO at run time, since it calls toString() on Object
	}
}