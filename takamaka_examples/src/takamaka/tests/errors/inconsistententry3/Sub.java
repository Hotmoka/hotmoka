package takamaka.tests.errors.inconsistententry3;

import io.takamaka.code.annotations.Entry;

public class Sub extends Super {
	public @Entry(Sub.class) void m() {}
}