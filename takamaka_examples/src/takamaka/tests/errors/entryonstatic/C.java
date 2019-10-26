package takamaka.tests.errors.entryonstatic;

import io.takamaka.code.annotations.Entry;
import io.takamaka.lang.Contract;

public class C extends Contract {
	public static @Entry void m() {}
}