package io.hotmoka.tests.errors.inconsistentpayable1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Super extends Contract {
	public @Payable @FromContract void m() {}
}