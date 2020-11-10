package io.takamaka.tests.errors.inconsistentredpayable2;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.RedPayable;

public class Sub extends Super {
	public @RedPayable @FromContract void m() {}
}