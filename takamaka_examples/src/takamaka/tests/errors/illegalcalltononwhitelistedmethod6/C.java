package takamaka.tests.errors.illegalcalltononwhitelistedmethod6;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class C {

	public static String foo() {
		return test(new Object(), new Object());
	}

	private static String test(Object... args) {
		String s = Stream.of(args)
			.map(Objects::toString) // this will be KO but only at run time
			.collect(Collectors.joining());
		s.length(); // to avoid dead-code elimination

		return Stream.of(args)
			.map(Objects::toString) // this will be KO but only at run time
			.collect(Collectors.joining());
	}
}