package io.hotmoka.examples.payablefailure;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;

public class C extends Contract {
	
	public void foo(C c) {
		require(c != null, "parameter cannot be null");
	}

	public @FromContract @Payable void goo(long amount, C c) {
		// if the next line throws an exception, the amount must be given back to the caller
		require(c != null, "parameter cannot be null");
	}
}