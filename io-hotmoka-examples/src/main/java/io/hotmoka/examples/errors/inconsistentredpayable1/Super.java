package io.hotmoka.examples.errors.inconsistentredpayable1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.RedPayable;

public class Super extends Contract {
	public @RedPayable @FromContract void m() {}
}