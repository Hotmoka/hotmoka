package io.hotmoka.tests.errors.exceptions;

import java.util.Optional;

public class C {

	public static void foo1() {
		// the following goes into an exception inside the Java library
		Optional.of(null);
	}

	public static void foo2(Object o) {
		// the following goes into an exception if o is null, but inside the Takamaka code
		o.hashCode();
	}
}