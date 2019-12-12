package io.takamaka.tests.errors.redpayablewithoutamount1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.RedPayable;

public class RedPayableWithoutAmount extends Contract {
	public @RedPayable @Entry void m() {};
}