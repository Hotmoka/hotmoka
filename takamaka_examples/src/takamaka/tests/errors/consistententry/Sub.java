package takamaka.tests.errors.consistententry;

import io.takamaka.code.lang.Entry;

public class Sub extends Super {
	public @Entry(Super.class) void m() {}
}