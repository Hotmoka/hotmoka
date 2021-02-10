package io.hotmoka.examples.errors.redpayablewithoutamount1;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.RedGreenContract;
import io.takamaka.code.lang.RedPayable;

public class RedPayableWithoutAmount extends RedGreenContract {
	public @RedPayable @FromContract void m() {};
}