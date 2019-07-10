package takamaka.tests.errors.illegalcalltoentry1;

import takamaka.lang.Contract;
import takamaka.lang.Entry;

public class C extends Contract {

	public @Entry void entry() {}

	public static void m() {
		new C().entry(); // KO
	}
}