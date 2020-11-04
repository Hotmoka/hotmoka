package io.takamaka.tests.errors.callernotonthis;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {
	private C caller;

	public @FromContract(C.class) C() {
		this.caller = (C) caller(); // ok
	}

	public void m() {
		caller.caller(); // ko
	}
}