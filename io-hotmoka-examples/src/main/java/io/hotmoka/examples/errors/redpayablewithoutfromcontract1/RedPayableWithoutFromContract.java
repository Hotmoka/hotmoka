package io.hotmoka.examples.errors.redpayablewithoutfromcontract1;

import io.takamaka.code.lang.RedGreenContract;
import io.takamaka.code.lang.RedPayable;

public class RedPayableWithoutFromContract extends RedGreenContract {
	public @RedPayable void m(int amount) {};
}