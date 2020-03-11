package io.takamaka.tests.errors.payablewithoutentry2;

import io.takamaka.code.lang.Payable;

public interface PayableWithoutEntry {
	public @Payable void m(int amount);
}