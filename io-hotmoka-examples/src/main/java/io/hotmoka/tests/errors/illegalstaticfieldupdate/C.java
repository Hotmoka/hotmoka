package io.hotmoka.tests.errors.illegalstaticfieldupdate;

public class C {

	public static int i;

	public C() {
		i++;
	}
}
