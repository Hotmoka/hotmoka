package io.hotmoka.tests.errors.inconsistentpayable2;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class Super extends Contract {
	public @FromContract void m() {}
}