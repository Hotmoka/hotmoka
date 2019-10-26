package takamaka.tests.errors.consistententry;

import io.takamaka.lang.Contract;
import takamaka.lang.Entry;

public class Super extends Contract {
	public @Entry(Sub.class) void m() {}
}