package io.hotmoka.tests.errors.illegalcalltofromcontract3;

import java.util.stream.Stream;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {

	public @FromContract void entry() {}

	public static void m() {
		String[] arr = { "hello", "how", "are", "you" };
		C c = new C();
		Stream.of(arr)
			.forEachOrdered(s -> c.entry());
	}
}