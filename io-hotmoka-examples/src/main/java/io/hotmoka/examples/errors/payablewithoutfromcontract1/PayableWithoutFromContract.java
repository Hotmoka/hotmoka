package io.hotmoka.examples.errors.payablewithoutfromcontract1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Payable;

public class PayableWithoutFromContract extends Contract {
	public @Payable void m(int amount) {}
}