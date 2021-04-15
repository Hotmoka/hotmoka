package io.hotmoka.examples.errors.redpayablewithoutfromcontract1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.RedPayable;

public class RedPayableWithoutFromContract extends Contract {
	public @RedPayable void m(int amount) {}
}