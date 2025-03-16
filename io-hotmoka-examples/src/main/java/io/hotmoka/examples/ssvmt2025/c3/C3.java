package io.hotmoka.examples.ssvmt2025.c3;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;

public class C3 extends Contract {
	private C3 owner;
	@SuppressWarnings("unused")
	private Contract c;

	public @FromContract(C3.class) C3() {
		owner = (C3) caller(); // ok
	}

	public @FromContract void m() {
		c = owner.caller();
	}
}