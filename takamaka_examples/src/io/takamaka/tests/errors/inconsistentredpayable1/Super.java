package io.takamaka.tests.errors.inconsistentredpayable1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.RedPayable;

public class Super extends Contract {
	public @RedPayable @Entry void m() {}
}