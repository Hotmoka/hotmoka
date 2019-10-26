package takamaka.tests.errors.illegalcalltoentry1;

import io.takamaka.code.annotations.Entry;
import io.takamaka.lang.Contract;

public class C extends Contract {

	public @Entry void entry() {}

	public static void m() {
		new C().entry(); // KO
	}
}