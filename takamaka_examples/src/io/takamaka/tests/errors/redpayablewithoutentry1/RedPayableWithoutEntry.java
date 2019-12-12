package io.takamaka.tests.errors.redpayablewithoutentry1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.RedPayable;

public class RedPayableWithoutEntry extends Contract {
	public @RedPayable void m(int amount) {};
}