package io.hotmoka.tests.errors.payablewithoutamount1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class PayableWithoutAmount extends Contract {
	public @Payable @FromContract void m() {};
}