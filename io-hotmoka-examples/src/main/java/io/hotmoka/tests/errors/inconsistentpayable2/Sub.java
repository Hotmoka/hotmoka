package io.hotmoka.tests.errors.inconsistentpayable2;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Sub extends Super {
	public @Payable @FromContract void m() {}
}