package takamaka.tests.errors.inconsistententry3;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;

public class Super extends Contract {
	public @Entry(Super.class) void m() {}
}