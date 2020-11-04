package io.takamaka.tests.errors.illegalcalltofromcontract5;

import java.util.stream.Stream;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {

	public @FromContract void entry(String s) {}

	public static void m() {
		String[] arr = { "hello", "how", "are", "you" };
		C c = new C();
		Stream.of(arr)
			.forEachOrdered(s -> {
				if (s.length() > 3)
					Stream.of(arr).forEachOrdered(c::entry);
			});
	}
}