package io.takamaka.tests.errors.redpayablewithoutentry2;

import io.takamaka.code.lang.RedPayable;

public interface RedPayableWithoutEntry {
	public @RedPayable void m(int amount);
}