package io.takamaka.tests.errors.redpayableinsimplecontract;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.RedPayable;

public class RedPayableInSimpleContract extends Contract { // should be RedGreenContract
	public @RedPayable @Entry void m(int amount) {};
}