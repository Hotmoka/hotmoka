package io.hotmoka.tests.errors.redpayableinsimplecontract;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.RedPayable;

public class RedPayableInSimpleContract extends Contract { // should be RedGreenContract
	public @RedPayable @FromContract void m(int amount) {};
}