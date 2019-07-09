package takamaka.tests.errors.entryonstatic;

import takamaka.lang.Contract;
import takamaka.lang.Entry;

public class C extends Contract {
	public static @Entry void m() {}
}