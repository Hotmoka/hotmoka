package io.takamaka.tests.errors.inconsistentpayable1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;

public class Super extends Contract {
	public @Payable @Entry void m() {}
}