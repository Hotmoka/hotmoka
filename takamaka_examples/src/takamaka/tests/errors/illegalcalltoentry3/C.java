package takamaka.tests.errors.illegalcalltoentry3;

import java.util.stream.Stream;

import io.takamaka.code.annotations.Entry;
import io.takamaka.lang.Contract;

public class C extends Contract {

	public @Entry void entry() {}

	public static void m() {
		String[] arr = { "hello", "how", "are", "you" };
		C c = new C();
		Stream.of(arr)
			.forEachOrdered(s -> c.entry());
	}
}