package io.takamaka.tests.errors.payablewithoutamount2;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class PayableWithoutAmount extends Contract {
	public @Payable @FromContract void m(float amount) {};
}