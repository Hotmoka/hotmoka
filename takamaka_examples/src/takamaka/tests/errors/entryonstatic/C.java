package takamaka.tests.errors.entryonstatic;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;

public class C extends Contract {
	public static @Entry void m() {}
}