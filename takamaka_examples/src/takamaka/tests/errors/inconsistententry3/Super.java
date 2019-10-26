package takamaka.tests.errors.inconsistententry3;

import io.takamaka.code.annotations.Entry;
import io.takamaka.lang.Contract;

public class Super extends Contract {
	public @Entry(Super.class) void m() {}
}