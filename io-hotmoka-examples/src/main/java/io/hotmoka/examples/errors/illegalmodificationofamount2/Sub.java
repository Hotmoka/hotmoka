package io.hotmoka.examples.errors.illegalmodificationofamount2;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class Sub extends Super {

	// cannot call the constructor of the superclass with something different from amount
	public @FromContract @Payable Sub(int amount) {
		super(++amount);
	}
}