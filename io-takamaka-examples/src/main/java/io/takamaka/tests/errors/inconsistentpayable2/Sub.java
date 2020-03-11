package io.takamaka.tests.errors.inconsistentpayable2;

import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;

public class Sub extends Super {
	public @Payable @Entry void m() {}
}