package io.hotmoka.examples.errors.payablewithoutfromcontract2;

import io.takamaka.code.lang.Payable;

public interface PayableWithoutFromContract {
	public @Payable void m(int amount);
}