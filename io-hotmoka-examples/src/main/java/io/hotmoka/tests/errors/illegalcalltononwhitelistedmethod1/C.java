package io.hotmoka.tests.errors.illegalcalltononwhitelistedmethod1;

public class C {

	public long foo() {
		return System.currentTimeMillis(); // KO
	}
}