package io.takamaka.tests.errors.redpayablewithoutentry1;

import io.takamaka.code.lang.RedGreenContract;
import io.takamaka.code.lang.RedPayable;

public class RedPayableWithoutEntry extends RedGreenContract {
	public @RedPayable void m(int amount) {};
}