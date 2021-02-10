package io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod1;

public class C {

	public long foo() {
		return System.currentTimeMillis(); // KO
	}
}