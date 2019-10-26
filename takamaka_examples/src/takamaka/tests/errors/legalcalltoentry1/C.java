package takamaka.tests.errors.legalcalltoentry1;

import java.util.stream.Stream;

import io.takamaka.lang.Contract;
import takamaka.lang.Entry;

public class C extends Contract {

	public @Entry void entry(String s) {}

	public void m() {
		String[] arr = { "hello", "how", "are", "you" };
		C c = new C();
		Stream.of(arr)
			.forEachOrdered(s -> {
				if (s.length() > 3)
					Stream.of(arr).forEachOrdered(c::entry);
			});
	}
}