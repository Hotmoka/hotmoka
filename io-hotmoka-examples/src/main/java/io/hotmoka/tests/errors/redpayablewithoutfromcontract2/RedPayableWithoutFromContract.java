package io.hotmoka.tests.errors.redpayablewithoutfromcontract2;

import io.takamaka.code.lang.RedPayable;

public interface RedPayableWithoutFromContract {
	public @RedPayable void m(int amount);
}