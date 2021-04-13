package io.hotmoka.examples.errors.payablewithoutfromcontract2;

import io.takamaka.code.lang.Payable;

public interface PayableWithoutFromContract {
	@Payable void m(int amount);
}