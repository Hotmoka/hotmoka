package takamaka.tests.errors.illegalcalltoentry4;

import java.util.stream.Stream;

import takamaka.lang.Contract;
import takamaka.lang.Entry;

public class C extends Contract {

	public @Entry void entry(String s) {}

	public static void m() {
		String[] arr = { "hello", "how", "are", "you" };
		C c = new C();
		Stream.of(arr)
			.forEachOrdered(c::entry);
	}
}