package io.hotmoka.examples.errors.redpayablewithoutamount1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.RedPayable;

public class RedPayableWithoutAmount extends Contract {
	public @RedPayable @FromContract void m() {};
}