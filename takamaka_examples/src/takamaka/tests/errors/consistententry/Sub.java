package takamaka.tests.errors.consistententry;

import takamaka.lang.Entry;

public class Sub extends Super {
	public @Entry(Super.class) void m() {}
}