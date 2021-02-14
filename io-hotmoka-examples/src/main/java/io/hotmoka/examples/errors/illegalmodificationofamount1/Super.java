package io.hotmoka.examples.errors.illegalmodificationofamount1;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Super extends Contract {
	public @FromContract @Payable Super(int amount) {}
}