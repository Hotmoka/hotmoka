package io.hotmoka.examples.errors.redpayablewithoutfromcontract2;

import io.takamaka.code.lang.RedPayable;

public interface RedPayableWithoutFromContract {
	@RedPayable void m(int amount);
}