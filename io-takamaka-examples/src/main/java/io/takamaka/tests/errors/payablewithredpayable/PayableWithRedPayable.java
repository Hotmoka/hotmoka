package io.takamaka.tests.errors.payablewithredpayable;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.RedPayable;

public class PayableWithRedPayable extends Contract {
	public @Payable @RedPayable void m(int amount) {};
}