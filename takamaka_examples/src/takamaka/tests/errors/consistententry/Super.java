package takamaka.tests.errors.consistententry;

import io.takamaka.code.annotations.Entry;
import io.takamaka.code.lang.Contract;

public class Super extends Contract {
	public @Entry(Sub.class) void m() {}
}