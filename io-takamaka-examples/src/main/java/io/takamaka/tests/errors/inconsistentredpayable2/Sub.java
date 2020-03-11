package io.takamaka.tests.errors.inconsistentredpayable2;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.RedPayable;

public class Sub extends Super {
	public @RedPayable @Entry void m() {}
}