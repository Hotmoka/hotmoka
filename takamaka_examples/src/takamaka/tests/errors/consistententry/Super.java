package takamaka.tests.errors.consistententry;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;

public class Super extends Contract {
	public @Entry(Sub.class) void m() {}
}