package io.hotmoka.tests.errors.callernotonthis;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C extends Contract {
	private C owner;

	public @FromContract(C.class) C() {
		this.owner = (C) caller(); // ok
	}

	public @FromContract void m() {
		owner.caller(); // ko
	}
}