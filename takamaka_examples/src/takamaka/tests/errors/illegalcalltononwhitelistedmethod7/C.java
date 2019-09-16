package takamaka.tests.errors.illegalcalltononwhitelistedmethod7;

public class C {

	public static String foo() {
		return test(new Object());
	}

	private static String test(Object arg) {
		String.valueOf(arg);
		return String.valueOf(arg); // KO at run time, since it calls toString() on Object
	}
}