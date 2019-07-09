package takamaka.tests.errors.inconsistententry3;

import takamaka.lang.Contract;
import takamaka.lang.Entry;

public class Super extends Contract {
	public @Entry(Super.class) void m() {}
}