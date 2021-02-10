package io.hotmoka.examples.errors.illegalcalltononwhitelistedmethod3;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class C {

	public static String foo() {
		return test(new Object(), new Object());
	}

	private static String test(Object... args) {
		return Stream.of(args)
			.map(Objects::toString) // this will be KO but only at run time
			.collect(Collectors.joining());
	}
}